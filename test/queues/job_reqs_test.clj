(ns queues.job-reqs-test
  (:require [clojure.spec.test.alpha :as stest]
            [clojure.test.check.clojure-test :refer [defspec]]
            [midje.sweet :refer :all]
            [queues.init :as init]
            [queues.logic.job-reqs :refer :all]
            [queues.specs.agent :as specs.agent]
            [queues.specs.job :as specs.job]
            [queues.specs.job-request :as specs.job-request]
            [queues.specs.job-queues :as specs.job-queues]))

(facts "queued-job-request"
       (fact "if job request is provided queue it in the end of job-requests-waiting
       queue on agents and jobs"
             (dosync
               (queued-job-request init/job-queues
                                   {::specs.job-request/agent-id "1"})
               init/job-queues
               => #(-> %
                       ::specs.job-queues/job-requests-waiting
                       deref
                       last
                       (= {::specs.job-request/agent-id "1"}))))
       (fact "if job request is provided adds one element to agents and jobs
       'job requests waiting' queue"
             (let [initial-count (-> init/job-queues
                                     ::specs.job-queues/job-requests-waiting
                                     deref
                                     count)]
               (dosync
                 (queued-job-request init/job-queues {::specs.job-request/agent-id "1"})
                 (count @init/job-requests-waiting) => (inc initial-count))))
       ;; TODO: convert this test to a property based test
       )
(facts "matching-waiting-job-request"
       (let [job {::specs.job/id "1" ::specs.job/type "bills" ::specs.job/urgent true}
             agent-1 {::specs.agent/primary-skillset ["rewards"] ::specs.agent/secondary-skillset [] ::specs.agent/id "1" ::specs.agent/name "a"}
             job-req-content-1 {::specs.job-request/agent-id (::specs.agent/id agent-1)}
             jqs-1 (assoc init/job-queues ::specs.job-queues/job-requests-waiting (ref [job-req-content-1])
                                          ::specs.job-queues/agents (agent [agent-1]))
             agent-2 {::specs.agent/primary-skillset ["bills"] ::specs.agent/secondary-skillset [] ::specs.agent/id "2" ::specs.agent/name "b"}
             job-req-content-2 {::specs.job-request/agent-id (::specs.agent/id agent-2)}
             jqs-2 (assoc init/job-queues ::specs.job-queues/job-requests-waiting (ref [job-req-content-2])
                                          ::specs.job-queues/agents (agent [agent-2]))
             agent-3 {::specs.agent/primary-skillset ["bills"] ::specs.agent/secondary-skillset [] ::specs.agent/id "3" ::specs.agent/name "c"}
             job-req-content-3 {::specs.job-request/agent-id (::specs.agent/id agent-3)}
             jqs-3 (assoc init/job-queues ::specs.job-queues/job-requests-waiting (ref [job-req-content-2 job-req-content-3])
                                          ::specs.job-queues/agents (agent [agent-2 agent-3]))]
         (fact "if there are no waiting job requirements return nil"
               (matching-waiting-job-request init/job-queues job) => nil)
         (fact "if there are no matching waiting job requirements return nil"
               (matching-waiting-job-request jqs-1 job) => nil)
         (fact "if there is a matching waiting job requirement returns the job requirement"
               (matching-waiting-job-request jqs-2 job) => job-req-content-2)
         (fact "if there are more then one job requirement that matches returns the first one in the coll"
               (matching-waiting-job-request jqs-3 job) => job-req-content-2)))

(stest/instrument)