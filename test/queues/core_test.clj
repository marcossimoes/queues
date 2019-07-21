(ns queues.core-test
  (:require [midje.sweet :refer :all]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]
            [cheshire.core :refer :all]
            [ring.mock.request :as mock]
            [queues.models.specs :as specs]
            [queues.core :refer :all]
            [queues.test :as test]))

(let [agents-and-jobs-scheme {::specs/agents               []
                              ::specs/jobs-assigned        []
                              ::specs/jobs-waiting         []
                              ::specs/job-requests-waiting []}]

  (let [new-agent-1 {::specs/new-agent {::specs/agent.id                 "8ab86c18-3fae-4804-bfd9-c3d6e8f66260",
                                         ::specs/agent.name               "BoJack Horseman",
                                         ::specs/agent.primary-skillset   ["bills-questions"],
                                         ::specs/agent.secondary-skillset []}}
        new-job-1 {::specs/new-job {::specs/job.id     "f26e890b-df8e-422e-a39c-7762aa0bac36",
                                     ::specs/job.type   "rewards-question",
                                     ::specs/job.urgent false}}
        new-job-2 {::specs/new-job {::specs/job.id     "c0033410-981c-428a-954a-35dec05ef1d2",
                                     ::specs/job.type   "bills-questions",
                                     ::specs/job.urgent true}}
        job-request {::specs/job-request {::specs/job-req.agent-id "8ab86c18-3fae-4804-bfd9-c3d6e8f66260"}}
        job-assigned {::specs/job-assigned {::specs/job-assigned.job-id  "c0033410-981c-428a-954a-35dec05ef1d2",
                                            ::specs/job-assigned.agent-id "8ab86c18-3fae-4804-bfd9-c3d6e8f66260"}}
        agent #:queues.models.specs{:agent.id                 "8ab86c18-3fae-4804-bfd9-c3d6e8f66260",
                                    :agent.name               "BoJack Horseman",
                                    :agent.primary-skillset   ["bills-questions"],
                                    :agent.secondary-skillset []}
        job #:queues.models.specs{:job.id     "f26e890b-df8e-422e-a39c-7762aa0bac36",
                                  :job.type   "rewards-question",
                                  :job.urgent false}]
    (facts "dequeue does not return a job-assigned until it has at least a new agent
       a new job and a job request that match each other"
           (fact "if dequeue receives an empty vector of events returns an empty vector"
                 (dequeue []) => [])
           (fact "if only receives a new agent or a new job returns an empty vector even if they are compatible"
                 (dequeue [new-agent-1]) => []
                 (dequeue [new-job-1]) => []
                 (dequeue [new-agent-1 new-job-1]) => [])
           (fact "if dequeue receives new agents, new jobs and job request that are compatible returns
       a job assigned"
                 (dequeue [new-agent-1 new-job-2 job-request]) => [job-assigned])
           (fact "if dequeue receives a new agent, a job-request from that agent and two new jobs
       that are compatible only assigns the first job"
                 (dequeue [new-agent-1 new-job-1 new-job-2 job-request]) => [job-assigned]))
    (facts "added-event"
           (fact "Adds new agents and new jobs to their respective queues in agents and jobs"
                 (added-event agents-and-jobs-scheme new-agent-1) => (contains {::specs/agents [agent]})
                 (added-event agents-and-jobs-scheme new-job-1) => (contains {::specs/jobs-waiting [job]}))))

  (facts "processed-job-req"
         (let [job-request-content {::specs/job-req.agent-id "8ab86c18-3fae-4804-bfd9-c3d6e8f66260"}
               job-content-1 {::specs/job.id     "f26e890b-df8e-422e-a39c-7762aa0bac36",
                              ::specs/job.type   "rewards-question",
                              ::specs/job.urgent false}
               job-content-2 {::specs/job.id     "c0033410-981c-428a-954a-35dec05ef1d2",
                              ::specs/job.type   "bills-questions",
                              ::specs/job.urgent true}
               job-assigned {::specs/job-assigned
                             {::specs/job-assigned.job-id   "c0033410-981c-428a-954a-35dec05ef1d2",
                              ::specs/job-assigned.agent-id "8ab86c18-3fae-4804-bfd9-c3d6e8f66260"}}
               aajs-with-new-agent (assoc agents-and-jobs-scheme ::specs/agents [{::specs/agent.id                 "8ab86c18-3fae-4804-bfd9-c3d6e8f66260",
                                                                                 ::specs/agent.name               "BoJack Horseman",
                                                                                 ::specs/agent.primary-skillset   ["bills-questions"],
                                                                                 ::specs/agent.secondary-skillset []}])
               aajs-with-new-job-1 (assoc aajs-with-new-agent ::specs/jobs-waiting [job-content-1])
               aajs-with-new-job-2 (assoc aajs-with-new-agent ::specs/jobs-waiting [job-content-2])]
           (fact "if no job is available for the agent returns job request queued in job-requests-waiting"
                 (processed-job-req agents-and-jobs-scheme job-request-content)
                 => (contains {::specs/job-requests-waiting [job-request-content]})
                 (processed-job-req aajs-with-new-job-1 job-request-content)
                 => (contains {::specs/job-requests-waiting [job-request-content]}))
           (fact "if a job is available for the agent returns job request assigned"
                 (processed-job-req aajs-with-new-job-2 job-request-content)
                 => (contains {::specs/jobs-assigned [job-assigned]}))))

  ;;TODO: improve this test (see queued-job-request): add count test and better contains

  (let [job (gen/generate (s/gen ::specs/job))
        agent (-> (gen/generate (s/gen ::specs/agent))
                  (assoc ::specs/agent.primary-skillset [(::specs/job.type job)]))
        job-req-content {::specs/job-req.agent-id (::specs/agent.id agent)}
        aajs-with-agents (-> agents-and-jobs-scheme
                             (update ::specs/agents conj agent))
        aajs-with-jobs-waiting (-> agents-and-jobs-scheme
                                   (update ::specs/agents conj agent)
                                   (update ::specs/jobs-waiting conj job))
        job-assigned {::specs/job-assigned {::specs/job-assigned.job-id   (::specs/job.id job)
                                            ::specs/job-assigned.agent-id (::specs/agent.id agent)}}
        job-assigned-aajs (update aajs-with-jobs-waiting ::specs/jobs-assigned conj job-assigned)]
    (facts "agent-found"
           (fact "if agents and jobs has the provided agent id returns agent"
                 (agent-found aajs-with-jobs-waiting job-req-content) => agent))
    (facts "matching waiting job"
           (fact "if there are no jobs in jobs-waiting that suit agent returns nil"
                 (matching-waiting-job aajs-with-agents job-req-content) => nil))
    (facts "update-job-assigneds-func"
           (fact
             (update aajs-with-jobs-waiting ::specs/jobs-assigned (update-job-assigneds-func job job-req-content))
             => job-assigned-aajs)))

  (facts "job-not-matches?"
         (fact "if both type and urgent matches return false"
               (let [] (job-not-matches? "rewards" true {::specs/job.id     "1"
                                                         ::specs/job.type   "rewards"
                                                         ::specs/job.urgent true})
                       => false))
         (fact "if type or urgent does not match returns true"
               (let [] (job-not-matches? "rewards" true {::specs/job.id     "2"
                                                         ::specs/job.type   "bills"
                                                         ::specs/job.urgent true})
                       => true)
               (let [] (job-not-matches? "rewards" true {::specs/job.id     "3"
                                                         ::specs/job.type   "rewards"
                                                         ::specs/job.urgent false})
                       => true))
         (fact "if both do not match match returns true"
               (let [] (job-not-matches? "rewards" true {::specs/job.id     "4"
                                                         ::specs/job.type   "bills"
                                                         ::specs/job.urgent false})
                       => true)))

  (let [agent {::specs/agent.primary-skillset ["rewards"] ::specs/agent.secondary-skillset ["bills"] ::specs/agent.id "1" ::specs/agent.name "a"}
        job-1 {::specs/job.id "1" ::specs/job.type "rewards" ::specs/job.urgent true}
        job-2 {::specs/job.id "2" ::specs/job.type "rewards" ::specs/job.urgent false}
        job-3 {::specs/job.id "3" ::specs/job.type "bills" ::specs/job.urgent true}
        job-4 {::specs/job.id "4" ::specs/job.type "bills" ::specs/job.urgent false}
        job-5 {::specs/job.id "5" ::specs/job.type "rewards" ::specs/job.urgent true}
        job-6 {::specs/job.id "6" ::specs/job.type "rewards" ::specs/job.urgent false}
        job-7 {::specs/job.id "7" ::specs/job.type "bills" ::specs/job.urgent true}
        job-8 {::specs/job.id "8" ::specs/job.type "bills" ::specs/job.urgent false}
        job-9 {::specs/job.id "9" ::specs/job.type "cb" ::specs/job.urgent true}
        job-10 {::specs/job.id "10" ::specs/job.type "cb" ::specs/job.urgent false}
        job-11 {::specs/job.id "11" ::specs/job.type "acq" ::specs/job.urgent true}
        job-12 {::specs/job.id "12" ::specs/job.type "acq" ::specs/job.urgent false}
        job-13 {::specs/job.id "13" ::specs/job.type "cb" ::specs/job.urgent true}
        job-14 {::specs/job.id "14" ::specs/job.type "cb" ::specs/job.urgent false}
        job-15 {::specs/job.id "15" ::specs/job.type "acq" ::specs/job.urgent true}
        job-16 {::specs/job.id "16" ::specs/job.type "acq" ::specs/job.urgent false}
        prim-true {::specs/priority.skill-type ::specs/agent.primary-skillset ::specs/priority.urgent true}
        second-true {::specs/priority.skill-type ::specs/agent.secondary-skillset ::specs/priority.urgent true}
        prim-false {::specs/priority.skill-type ::specs/agent.primary-skillset ::specs/priority.urgent false}
        second-false {::specs/priority.skill-type ::specs/agent.secondary-skillset ::specs/priority.urgent false}]
    (facts "job-with-prior"
           (fact "if no job matches either skill, urgency or both returns nil"
                 (job-with-prior agent [job-2 job-3 job-4] prim-true) => nil
                 (job-with-prior agent [job-1 job-2 job-4] second-true) => nil
                 (job-with-prior agent [job-3 job-4] prim-true) => nil
                 (job-with-prior agent [job-1 job-2] second-true) => nil
                 (job-with-prior agent [job-4] prim-true) => nil
                 (job-with-prior agent [job-2] second-true) => nil)
           (fact "if job matches both skill, urgency or both returns the job"
                 (job-with-prior agent [job-1 job-2 job-3 job-4] prim-true) => job-1
                 (job-with-prior agent [job-1 job-2 job-3 job-4] second-true) => job-3
                 (job-with-prior agent [job-1 job-2 job-3 job-4] prim-false) => job-2
                 (job-with-prior agent [job-1 job-2 job-3 job-4] second-false) => job-4)
           (fact "Handles agents that have no secondary skillset"
                 (job-with-prior {::specs/agent.primary-skillset   ["rewards"]
                                  ::specs/agent.secondary-skillset []
                                  ::specs/agent.id "2"
                                  ::specs/agent.name "b"}
                                 [job-1 job-2 job-3 job-4] second-true)
                 => nil)
           (fact "Returns the first and only the first matching job"
                 (job-with-prior agent [job-1 job-2 job-3 job-4 job-5] prim-true) => job-1
                 (job-with-prior agent [job-1 job-2 job-3 job-4 job-7] second-true) => job-3
                 (job-with-prior agent [job-1 job-2 job-3 job-4 job-6] prim-false) => job-2
                 (job-with-prior agent [job-1 job-2 job-3 job-4 job-8] second-false) => job-4))
    (facts "job-found"
           (fact "Returns nil if there are no jobs that match the agent skills"
                 (job-found [job-9 job-10 job-11 job-12 job-13 job-14 job-15 job-16] agent) => nil)
           (fact "Returns the first jobs with right priority.
       primary-urgent > primary > secondary-urgent > secondary"
                 (job-found [job-3 job-7 job-1 job-5 job-2 job-4 job-6 job-8] agent) => job-1
                 (job-found [job-3 job-7 job-2 job-4 job-6 job-8] agent) => job-2
                 (job-found [job-3 job-7 job-4 job-8] agent) => job-3
                 (job-found [job-4 job-8] agent) => job-4)
           (fact "Returns nil if there are no jobs in the waiting list"
                 (job-found [] agent) => nil)))
  (facts "queued-job-request"
         (fact "if job request is provided queue it in the end of job-requests-waiting
       queue on agents and jobs"
               (queued-job-request agents-and-jobs-scheme {::specs/job-req.agent-id "1"})
               => #(= {::specs/job-req.agent-id "1"} (last (::specs/job-requests-waiting %))))
         (fact "if job request is provided adds one element to agents and jobs
       'job requests waiting' queue"
               (-> agents-and-jobs-scheme
                   (queued-job-request {::specs/job-req.agent-id "1"})
                   (::specs/job-requests-waiting)
                   (count))
               => (-> agents-and-jobs-scheme
                      (::specs/job-requests-waiting)
                      (count)
                      (inc))))
  (facts "agent-skillsets"
         (fact "if agent does not have a secondary skill does not return nil for the skill missing"
               (agent-skillsets {::specs/agent.primary-skillset ["rewards-questions"]
                                 ::specs/agent.secondary-skillset []})
               => ["rewards-questions"])
         (fact "returns a vector with both skills in one single coll"
               (agent-skillsets {::specs/agent.primary-skillset ["rewards-questions"]
                                 ::specs/agent.secondary-skillset ["bills-questions"]})
               => ["rewards-questions" "bills-questions"]))
  (facts "matching-waiting-job-req"
         (let [job {::specs/job.id "1" ::specs/job.type "bills" ::specs/job.urgent true}
               agent-1 {::specs/agent.primary-skillset ["rewards"] ::specs/agent.secondary-skillset [] ::specs/agent.id "1" ::specs/agent.name "a" }
               job-req-content-1 {::specs/job-req.agent-id (::specs/agent.id agent-1)}
               aajs-1 (assoc agents-and-jobs-scheme ::specs/job-requests-waiting [job-req-content-1]
                                                    ::specs/agents               [agent-1])
               agent-2 {::specs/agent.primary-skillset ["bills"] ::specs/agent.secondary-skillset [] ::specs/agent.id "2" ::specs/agent.name "b" }
               job-req-content-2 {::specs/job-req.agent-id (::specs/agent.id agent-2)}
               aajs-2 (assoc agents-and-jobs-scheme ::specs/job-requests-waiting [job-req-content-2]
                                                    ::specs/agents               [agent-2])
               agent-3 {::specs/agent.primary-skillset ["bills"] ::specs/agent.secondary-skillset [] ::specs/agent.id "3" ::specs/agent.name "c" }
               job-req-content-3 {::specs/job-req.agent-id (::specs/agent.id agent-3)}
               aajs-3 (assoc agents-and-jobs-scheme ::specs/job-requests-waiting [job-req-content-2 job-req-content-3]
                                                    ::specs/agents               [agent-2 agent-3])
               ]
           (fact "if there are no waiting job requirements return nil"
                 (matching-waiting-job-req agents-and-jobs-scheme job) => nil)
           (fact "if there are no matching waiting job requirements return nil"
                 (matching-waiting-job-req aajs-1 job) => nil)
           (fact "if there is a matching waiting job requirement returns the job requirement"
                 (matching-waiting-job-req aajs-2 job) => job-req-content-2)
           (fact "if there are more then one job requirement that matches returns the first one in the coll"
                 (matching-waiting-job-req aajs-3 job) => job-req-content-2)))
  (facts "queued-job"
         (let [job {::specs/job.id "1" ::specs/job.type "bills" ::specs/job.urgent true}]
           (fact "if a job is provided it returns 'agents-and-jobs' with the job queued"
                 (queued-job agents-and-jobs-scheme job) => (contains {::specs/jobs-waiting [job]}))
           (fact "if a job is provided it queues it in the end of the 'jobs waiting' list
           in the 'agents-and-jobs' maps"
                 (let [aajs (assoc agents-and-jobs-scheme ::specs/jobs-waiting
                                                          [{::specs/job.id "2" ::specs/job.type "rewards" ::specs/job.urgent false}
                                                           {::specs/job.id "3" ::specs/job.type "bills" ::specs/job.urgent true}
                                                           {::specs/job.id "4" ::specs/job.type "rewards" ::specs/job.urgent false}])
                       last-job (->> job
                                     (queued-job aajs)
                                     (::specs/jobs-waiting)
                                     (last))]
                   last-job => job))))
  (facts "id-removed-from-vector"
         (fact "if it receives an id and a list of vectors with maps one of them containing that id, removes it"
           (let [res-func (id-removed-from-vector "1" ::specs/job.id)]
             (res-func [{::specs/job.id "1"} {::specs/job.id "2"} {::specs/job.id "3"} {::specs/job.id "4"}])
             => [{::specs/job.id "2"} {::specs/job.id "3"} {::specs/job.id "4"}])))

  (facts "assigned-job"
         (fact "If it receives an 'agents-and-jobs' map, a job request content and a job
         returns a new 'agents-and-jobs' maps with a new job-assigned event containing
         the previously inputed 'job-request-agent-id' and the 'job-id'"
           (assigned-job agents-and-jobs-scheme
                         {::specs/job-req.agent-id "2"}
                         {::specs/job.id "1" ::specs/job.type "bills" ::specs/job.urgent true})
               => (contains {::specs/jobs-assigned [{::specs/job-assigned {::specs/job-assigned.job-id      "1"
                                                                           ::specs/job-assigned.agent-id "2"}}]}))))

(facts "-main"
       (fact "receives a sample-input file and returns a sample-output file"
             (-main "resources/sample-input.json.txt")
             (slurp "jobs-assigned.json.txt") => (slurp "resources/sample-output.json.txt")))

(facts "agents end-point"
       (fact "receives a put request of an agent returns successful"
             (handled-agent (mock/request :put "/agents")) => (contains {:status 200})))

(defspec runs-with-out-erros-for-all-inputs
         100
         (prop/for-all [events (test/gen-events)]
                       (->> events
                            (dequeue))))

(defspec outputs-clj-formatted-job-assigned-agent-id-and-job-id
         100
         (prop/for-all [events (test/gen-events)]
                       (->> events
                            (dequeue)
                            (every? #(and (= ((comp first keys) %) ::specs/job-assigned)
                                          (= (set ((comp keys first vals) %)) #{::specs/job-assigned.job-id ::specs/job-assigned.agent-id}))))))

(defspec job-requests>=jobs-assigned
         100
         (prop/for-all [events (test/gen-events)]
                       (let [num-job-requests (reduce #(if (= ::specs/job-request ((comp first keys) %2))
                                                        (inc %1)
                                                        %)
                                                      0 events)
                             num-jobs-assigned (->> events (dequeue) (count))]
                         (>= num-job-requests num-jobs-assigned))))

(defspec jobs>=jobs-assigned
         100
         (prop/for-all [events (test/gen-events)]
                       (let [num-jobs (reduce #(if (= ::specs/new-job ((comp first keys) %2))
                                                 (inc %1)
                                                 %)
                                              0 events)
                             num-jobs-assigned (->> events (dequeue) (count))]
                         (>= num-jobs num-jobs-assigned))))

(stest/instrument)

;; TODO: implement error handling tests
;; TODO: move all the support generated values to the test.clj file