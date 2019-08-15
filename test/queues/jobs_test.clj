(ns queues.jobs-test
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.clojure-test :refer [defspec]]
            [midje.sweet :refer :all]
            [queues.init :as init]
            [queues.logic.jobs :refer :all]
            [queues.specs.agent :as specs.agent]
            [queues.specs.job :as specs.job]
            [queues.specs.job-request :as specs.job-request]
            [queues.specs.job-queues :as specs.job-queues]
            [queues.specs.priority-rule :as specs.priority-rule]))

(let [sample-job (gen/generate (s/gen ::specs.job/job))
      sample-agent (-> (gen/generate (s/gen ::specs.agent/agent))
                       (assoc ::specs.agent/primary-skillset [(::specs.job/type sample-job)]))
      job-req-payload {::specs.job-request/agent-id (::specs.agent/id sample-agent)}
      jqs-with-agents (-> init/job-queues
                          (assoc ::specs.job-queues/agents (agent [sample-agent])))]
  (facts "matching waiting job"
         (fact "if there are no jobs in jobs-waiting that suit agent returns nil"
               (matching-waiting-job jqs-with-agents job-req-payload) => nil)))
(facts "queued-job"
       (let [sample-job {::specs.job/id "1" ::specs.job/type "bills" ::specs.job/urgent true}]
         (fact "if a job is provided it returns 'job-queues' with the job queued"
               (dosync
                 (queued-job init/job-queues sample-job))
               (deref init/jobs-waiting) => (contains sample-job))
         (fact "if a job is provided it queues it in the end of the 'jobs waiting' list
           in the 'job-queues' maps"
               (dosync
                 (let [jqs (assoc init/job-queues
                             ::specs.job-queues/jobs-waiting
                             (ref [{::specs.job/id "2" ::specs.job/type "rewards" ::specs.job/urgent false}
                                   {::specs.job/id "3" ::specs.job/type "bills" ::specs.job/urgent true}
                                   {::specs.job/id "4" ::specs.job/type "rewards" ::specs.job/urgent false}]))]
                   (queued-job jqs sample-job)
                   (last @init/jobs-waiting) => sample-job)))))

(facts "job-not-matches?"
       (fact "if both type and urgent matches return false"
             (let [] (job-not-matches? "rewards" true {::specs.job/id     "1"
                                                       ::specs.job/type   "rewards"
                                                       ::specs.job/urgent true})
                     => false))
       (fact "if type or urgent does not match returns true"
             (let [] (job-not-matches? "rewards" true {::specs.job/id     "2"
                                                       ::specs.job/type   "bills"
                                                       ::specs.job/urgent true})
                     => true)
             (let [] (job-not-matches? "rewards" true {::specs.job/id     "3"
                                                       ::specs.job/type   "rewards"
                                                       ::specs.job/urgent false})
                     => true))
       (fact "if both do not match match returns true"
             (let [] (job-not-matches? "rewards" true {::specs.job/id     "4"
                                                       ::specs.job/type   "bills"
                                                       ::specs.job/urgent false})
                       => true)))

(let [sample-agent {::specs.agent/primary-skillset ["rewards"] ::specs.agent/secondary-skillset ["bills"] ::specs.agent/id "1" ::specs.agent/name "a"}
      job-1 {::specs.job/id "1" ::specs.job/type "rewards" ::specs.job/urgent true}
      job-2 {::specs.job/id "2" ::specs.job/type "rewards" ::specs.job/urgent false}
      job-3 {::specs.job/id "3" ::specs.job/type "bills" ::specs.job/urgent true}
      job-4 {::specs.job/id "4" ::specs.job/type "bills" ::specs.job/urgent false}
      job-5 {::specs.job/id "5" ::specs.job/type "rewards" ::specs.job/urgent true}
      job-6 {::specs.job/id "6" ::specs.job/type "rewards" ::specs.job/urgent false}
      job-7 {::specs.job/id "7" ::specs.job/type "bills" ::specs.job/urgent true}
      job-8 {::specs.job/id "8" ::specs.job/type "bills" ::specs.job/urgent false}
      job-9 {::specs.job/id "9" ::specs.job/type "cb" ::specs.job/urgent true}
      job-10 {::specs.job/id "10" ::specs.job/type "cb" ::specs.job/urgent false}
      job-11 {::specs.job/id "11" ::specs.job/type "acq" ::specs.job/urgent true}
      job-12 {::specs.job/id "12" ::specs.job/type "acq" ::specs.job/urgent false}
      job-13 {::specs.job/id "13" ::specs.job/type "cb" ::specs.job/urgent true}
      job-14 {::specs.job/id "14" ::specs.job/type "cb" ::specs.job/urgent false}
      job-15 {::specs.job/id "15" ::specs.job/type "acq" ::specs.job/urgent true}
      job-16 {::specs.job/id "16" ::specs.job/type "acq" ::specs.job/urgent false}
      prim-true {::specs.priority-rule/skill-type ::specs.agent/primary-skillset ::specs.priority-rule/urgent true}
      second-true {::specs.priority-rule/skill-type ::specs.agent/secondary-skillset ::specs.priority-rule/urgent true}
      prim-false {::specs.priority-rule/skill-type ::specs.agent/primary-skillset ::specs.priority-rule/urgent false}
      second-false {::specs.priority-rule/skill-type ::specs.agent/secondary-skillset ::specs.priority-rule/urgent false}]
  (facts "job-with-prior"
         (fact "if no job matches either skill, urgency or both returns nil"
               (job-with-prior sample-agent (ref [job-2 job-3 job-4]) prim-true) => nil
               (job-with-prior sample-agent (ref [job-1 job-2 job-4]) second-true) => nil
               (job-with-prior sample-agent (ref [job-3 job-4]) prim-true) => nil
               (job-with-prior sample-agent (ref [job-1 job-2]) second-true) => nil
               (job-with-prior sample-agent (ref [job-4]) prim-true) => nil
               (job-with-prior sample-agent (ref [job-2]) second-true) => nil)
         (fact "if job matches both skill, urgency or both returns the job"
               (job-with-prior sample-agent (ref [job-1 job-2 job-3 job-4]) prim-true) => job-1
               (job-with-prior sample-agent (ref [job-1 job-2 job-3 job-4]) second-true) => job-3
               (job-with-prior sample-agent (ref [job-1 job-2 job-3 job-4]) prim-false) => job-2
               (job-with-prior sample-agent (ref [job-1 job-2 job-3 job-4]) second-false) => job-4)
         (fact "Handles agents that have no secondary skillset"
               (job-with-prior {::specs.agent/primary-skillset   ["rewards"]
                                ::specs.agent/secondary-skillset []
                                ::specs.agent/id "2"
                                ::specs.agent/name "b"}
                               (ref [job-1 job-2 job-3 job-4]) second-true)
               => nil)
         (fact "Returns the first and only the first matching job"
               (job-with-prior sample-agent (ref [job-1 job-2 job-3 job-4 job-5]) prim-true) => job-1
               (job-with-prior sample-agent (ref [job-1 job-2 job-3 job-4 job-7]) second-true) => job-3
               (job-with-prior sample-agent (ref [job-1 job-2 job-3 job-4 job-6]) prim-false) => job-2
               (job-with-prior sample-agent (ref [job-1 job-2 job-3 job-4 job-8]) second-false) => job-4))
  (facts "job-found"
         (fact "Returns nil if there are no jobs that match the agent skills"
               (job-found (ref [job-9 job-10 job-11 job-12 job-13 job-14 job-15 job-16])
                          sample-agent
                          init/priority-rules) => nil)
         (fact "Returns the first jobs with right priority.
       primary-urgent > primary > secondary-urgent > secondary"
               (job-found (ref [job-3 job-7 job-1 job-5 job-2 job-4 job-6 job-8])
                          sample-agent
                          init/priority-rules) => job-1
               (job-found (ref [job-3 job-7 job-2 job-4 job-6 job-8])
                          sample-agent
                          init/priority-rules) => job-2
               (job-found (ref [job-3 job-7 job-4 job-8])
                          sample-agent
                          init/priority-rules) => job-3
               (job-found (ref [job-4 job-8])
                          sample-agent
                          init/priority-rules) => job-4)
         (fact "Returns nil if there are no jobs in the waiting list"
               (job-found (ref [])
                          sample-agent
                          init/priority-rules) => nil)))

(stest/instrument)