(ns queues.core-test
  (:require [midje.sweet :refer :all]
            [queues.core :refer :all]
            [queues.models.events :as events]
            [queues.models.agent :as agent]
            [queues.models.job :as job]
            [queues.models.agents-and-jobs :as aajs]
            [queues.models.job-assigned :as ja]
            [queues.models.job-request :as jr]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]))

(let [agents-and-jobs-scheme {::aajs/agents               []
                              ::aajs/jobs-assigned        []
                              ::aajs/jobs-waiting         []
                              ::aajs/job-requests-waiting []}]

  (let [new-agent-1 {::events/new-agent {"id"                 "8ab86c18-3fae-4804-bfd9-c3d6e8f66260",
                                         "name"               "BoJack Horseman",
                                         "primary_skillset"   ["bills-questions"],
                                         "secondary_skillset" []}}
        new-job-1 {::events/new-job {"id"     "f26e890b-df8e-422e-a39c-7762aa0bac36",
                                     "type"   "rewards-question",
                                     "urgent" false}}
        new-job-2 {::events/new-job {"id"     "c0033410-981c-428a-954a-35dec05ef1d2",
                                     "type"   "bills-questions",
                                     "urgent" true}}
        job-request {::events/job-request {"agent_id" "8ab86c18-3fae-4804-bfd9-c3d6e8f66260"}}
        job-assigned {::ja/job-assigned {::job/id   "c0033410-981c-428a-954a-35dec05ef1d2",
                                         ::jr/agent-id "8ab86c18-3fae-4804-bfd9-c3d6e8f66260"}}
        agent #:queues.models.agent{:id                 "8ab86c18-3fae-4804-bfd9-c3d6e8f66260",
                                    :name               "BoJack Horseman",
                                    :primary-skillset   ["bills-questions"],
                                    :secondary-skillset []}
        job #:queues.models.job{:id     "f26e890b-df8e-422e-a39c-7762aa0bac36",
                                :type   "rewards-question",
                                :urgent false}]
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
                 (added-event agents-and-jobs-scheme new-agent-1) => (contains {::aajs/agents [agent]})
                 (added-event agents-and-jobs-scheme new-job-1) => (contains {::aajs/jobs-waiting [job]}))))

  (facts "processed-job-req"
         (let [job-request-content {::jr/agent-id "8ab86c18-3fae-4804-bfd9-c3d6e8f66260"}
               job-content-1 {::job/id     "f26e890b-df8e-422e-a39c-7762aa0bac36",
                              ::job/type   "rewards-question",
                              ::job/urgent false}
               job-content-2 {::job/id     "c0033410-981c-428a-954a-35dec05ef1d2",
                              ::job/type   "bills-questions",
                              ::job/urgent true}
               job-assigned {::ja/job-assigned
                             {::job/id   "c0033410-981c-428a-954a-35dec05ef1d2",
                              ::jr/agent-id "8ab86c18-3fae-4804-bfd9-c3d6e8f66260"}}
               aajs-with-new-agent (assoc agents-and-jobs-scheme ::aajs/agents [{::agent/id                 "8ab86c18-3fae-4804-bfd9-c3d6e8f66260",
                                                                                 ::agent/name               "BoJack Horseman",
                                                                                 ::agent/primary-skillset   ["bills-questions"],
                                                                                 ::agent/secondary-skillset []}])
               aajs-with-new-job-1 (assoc aajs-with-new-agent ::aajs/jobs-waiting [job-content-1])
               aajs-with-new-job-2 (assoc aajs-with-new-agent ::aajs/jobs-waiting [job-content-2])]
           (fact "if no job is available for the agent returns job request queued in job-requests-waiting"
                 (processed-job-req agents-and-jobs-scheme job-request-content)
                 => (contains {::aajs/job-requests-waiting [job-request-content]})
                 (processed-job-req aajs-with-new-job-1 job-request-content)
                 => (contains {::aajs/job-requests-waiting [job-request-content]}))
           (fact "if a job is available for the agent returns job request assigned"
                 (processed-job-req aajs-with-new-job-2 job-request-content)
                 => (contains {::aajs/jobs-assigned [job-assigned]}))))

  ;;TODO: improve this test (see queued-job-request): add count test and better contains

  (let [job (gen/generate (s/gen ::job/job))
        agent (-> (gen/generate (s/gen ::agent/agent))
                  (assoc ::agent/primary-skillset [(::job/type job)]))
        job-req-content {::jr/agent-id (::agent/id agent)}
        aajs-with-agents (-> agents-and-jobs-scheme
                             (update ::aajs/agents conj agent))
        aajs-with-jobs-waiting (-> agents-and-jobs-scheme
                                   (update ::aajs/agents conj agent)
                                   (update ::aajs/jobs-waiting conj job))
        job-assigned {::ja/job-assigned {::job/id   (::job/id job)
                                         ::jr/agent-id (::agent/id agent)}}
        job-assigned-aajs (update aajs-with-jobs-waiting ::aajs/jobs-assigned conj job-assigned)]
    (facts "agent-found"
           (fact "if agents and jobs has the provided agent id returns agent"
                 (agent-found aajs-with-jobs-waiting job-req-content) => agent))
    (facts "matching waiting job"
           (fact "if there are no jobs in jobs-waiting that suit agent returns nil"
                 (matching-waiting-job aajs-with-agents job-req-content) => nil))
    (facts "update-job-assigneds-func"
           (fact
             (update aajs-with-jobs-waiting ::aajs/jobs-assigned (update-job-assigneds-func job job-req-content))
             => job-assigned-aajs)))

  (facts "job-not-matches?"
         (fact "if both type and urgent matches return false"
               (let [] (job-not-matches? "rewards" true {::job/type   "rewards"
                                                         ::job/urgent true})
                       => false))
         (fact "if type or urgent does not match returns true"
               (let [] (job-not-matches? "rewards" true {::job/type   "bills"
                                                         ::job/urgent true})
                       => true)
               (let [] (job-not-matches? "rewards" true {::job/type   "rewards"
                                                         ::job/urgent false})
                       => true))
         (fact "if both do not match match returns true"
               (let [] (job-not-matches? "rewards" true {::job/type   "bills"
                                                         ::job/urgent false})
                       => true)))

  (let [agent {::agent/primary-skillset ["rewards"] ::agent/secondary-skillset ["bills"]}
        job-1 {::job/id 1 ::job/type "rewards" ::job/urgent true}
        job-2 {::job/id 2 ::job/type "rewards" ::job/urgent false}
        job-3 {::job/id 3 ::job/type "bills" ::job/urgent true}
        job-4 {::job/id 4 ::job/type "bills" ::job/urgent false}
        job-5 {::job/id 5 ::job/type "rewards" ::job/urgent true}
        job-6 {::job/id 6 ::job/type "rewards" ::job/urgent false}
        job-7 {::job/id 7 ::job/type "bills" ::job/urgent true}
        job-8 {::job/id 8 ::job/type "bills" ::job/urgent false}
        job-9 {::job/id 9 ::job/type "cb" ::job/urgent true}
        job-10 {::job/id 10 ::job/type "cb" ::job/urgent false}
        job-11 {::job/id 11 ::job/type "acq" ::job/urgent true}
        job-12 {::job/id 12 ::job/type "acq" ::job/urgent false}
        job-13 {::job/id 13 ::job/type "cb" ::job/urgent true}
        job-14 {::job/id 14 ::job/type "cb" ::job/urgent false}
        job-15 {::job/id 15 ::job/type "acq" ::job/urgent true}
        job-16 {::job/id 16 ::job/type "acq" ::job/urgent false}
        prim-true {:skill-type ::agent/primary-skillset :urgent true}
        second-true {:skill-type ::agent/secondary-skillset :urgent true}
        prim-false {:skill-type ::agent/primary-skillset :urgent false}
        second-false {:skill-type ::agent/secondary-skillset :urgent false}]
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
                 (job-with-prior {::agent/primary-skillset   ["rewards"]
                                  ::agent/secondary-skillset []}
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
               (queued-job-request agents-and-jobs-scheme {::agent/id 1})
               => #(= {::agent/id 1} (last (::aajs/job-requests-waiting %))))
         (fact "if job request is provided adds one element to agents and jobs
       'job requests waiting' queue"
               (-> agents-and-jobs-scheme
                   (queued-job-request {::agent/id 1})
                   (::aajs/job-requests-waiting)
                   (count))
               => (-> agents-and-jobs-scheme
                      (::aajs/job-requests-waiting)
                      (count)
                      (inc))))
  (facts "agent-skillsets"
         (fact "if agent does not have a secondary skill does not return nil for the skill missing"
               (agent-skillsets {::agent/primary-skillset ["rewards-questions"]
                                 ::agent/secondary-skillset []})
               => ["rewards-questions"])
         (fact "returns a vector with both skills in one single coll"
               (agent-skillsets {::agent/primary-skillset ["rewards-questions"]
                                 ::agent/secondary-skillset ["bills-questions"]})
               => ["rewards-questions" "bills-questions"]))
  (facts "matching-waiting-job-req"
         (let [job {::job/id 1 ::job/type "bills" ::job/urgent true}
               agent-1 {::agent/primary-skillset ["rewards"] ::agent/secondary-skillset []}
               job-req-content-1 {::jr/agent-id (::agent/id agent-1)}
               aajs-1 (assoc agents-and-jobs-scheme ::aajs/job-requests-waiting [job-req-content-1]
                                                    ::aajs/agents               [agent-1])
               agent-2 {::agent/primary-skillset ["bills"] ::agent/secondary-skillset []}
               job-req-content-2 {::jr/agent-id (::agent/id agent-1)}
               aajs-2 (assoc agents-and-jobs-scheme ::aajs/job-requests-waiting [job-req-content-2]
                                                    ::aajs/agents               [agent-2])
               agent-3 {::agent/primary-skillset ["bills"] ::agent/secondary-skillset []}
               job-req-content-3 {::jr/agent-id (::agent/id agent-3)}
               aajs-3 (assoc agents-and-jobs-scheme ::aajs/job-requests-waiting [job-req-content-2 job-req-content-3]
                                                    ::aajs/agents               [agent-2 agent-3])
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
         (let [job {::job/id 1 ::job/type "bills" ::job/urgent true}]
           (fact "if a job is provided it returns 'agent-and-jobs' with the job queued"
                 (queued-job agents-and-jobs-scheme job) => (contains {::aajs/jobs-waiting [job]}))
           (fact "if a job is provided it queues it in the end of the 'jobs waiting' list
           in the 'agents-and-jobs' maps"
                 (let [aajs (assoc agents-and-jobs-scheme ::aajs/jobs-waiting
                                                          [{::job/id 2 ::job/type "rewards" ::job/urgent false}
                                                           {::job/id 3 ::job/type "bills" ::job/urgent true}
                                                           {::job/id 4 ::job/type "rewards" ::job/urgent false}])
                       last-job (->> job
                                     (queued-job aajs)
                                     (::aajs/jobs-waiting)
                                     (last))]
                   last-job => job))))
  (facts "js-kw->cj-kw"
         (fact "receives keys as strings and returns them as keywords"
               (js-kw->cj-kw "my" "name") => :my/name)
         (fact "if keys are composed by _ replaces them for -"
               (js-kw->cj-kw "my" "first_name") => :my/first-name))
  (facts "namespaced-kws-content"
         (let [agent-js {::events/new-agent {"id"                 "8ab86c18-3fae-4804-bfd9-c3d6e8f66260",
                                             "name"               "BoJack Horseman",
                                             "primary_skillset"   ["bills-questions"],
                                             "secondary_skillset" []}}
               agent-clj {::agent/id               "8ab86c18-3fae-4804-bfd9-c3d6e8f66260",
                          ::agent/name             "BoJack Horseman",
                          ::agent/primary-skillset ["bills-questions"],
                          ::agent/secondary-skillset []}
               job-js {::events/new-job {"id"     "f26e890b-df8e-422e-a39c-7762aa0bac36",
                                         "type"   "rewards-question",
                                         "urgent" false}}
               job-clj {::job/id     "f26e890b-df8e-422e-a39c-7762aa0bac36",
                        ::job/type   "rewards-question",
                        ::job/urgent false}
               job-request-js {::events/job-request {"agent_id" "8ab86c18-3fae-4804-bfd9-c3d6e8f66260"}}
               job-request-clj {::jr/agent-id "8ab86c18-3fae-4804-bfd9-c3d6e8f66260"}]
           (fact "If it receives a namespace and a json formatted event content transforms it to a clj formatted event"
             (namespaced-kws-content "queues.models.agent" agent-js) => agent-clj
             (namespaced-kws-content "queues.models.job" job-js) => job-clj
             (namespaced-kws-content "queues.models.job-request" job-request-js) => job-request-clj)))
  (facts "id-removed-from-vector"
         (fact "if it receives an id and a list of vectors with maps one of them containing that id, removes it"
           (let [res-func (id-removed-from-vector "1")]
             (println res-func)
             (res-func [{::job/id "1"} {::job/id "2"} {::job/id "3"} {::job/id "4"}])
             => [{::job/id "2"} {::job/id "3"} {::job/id "4"}])))
  (facts "assigned-job"
         (fact "If it receives an 'agents-and-jobs' map, a job request content and a job
         returns a new 'agents-and-jobs' maps with a new job-assigned event containing
         the previously inputed 'job-request-agent-id' and the 'job-id'"
           (assigned-job agents-and-jobs-scheme {::jr/agent-id "2"} {::job/id "1"})
               => (contains {::aajs/jobs-assigned [{::ja/job-assigned {::job/id      "1"
                                                                       ::jr/agent-id "2"}}]}))))

;; TODO: implement error handling tests