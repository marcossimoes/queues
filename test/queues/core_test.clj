(ns queues.core-test
  (:require [midje.sweet :refer :all]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]
            [cheshire.core :refer :all]
            [queues.core :refer :all]
            [queues.fixtures :as fix]
            [queues.init :as init]
            [queues.specs.job :as specs.job]
            [queues.specs.agent :as specs.agent]
            [queues.specs.events :as specs.events]
            [queues.specs.job-request :as specs.job-request]
            [queues.specs.job-assigned :as specs.job-assigned]
            [queues.specs.job-queues :as specs.job-queues]))

(let [new-agent-1 {::specs.events/new-agent {::specs.agent/id                 "8ab86c18-3fae-4804-bfd9-c3d6e8f66260",
                                             ::specs.agent/name               "BoJack Horseman",
                                             ::specs.agent/primary-skillset   ["bills-questions"],
                                             ::specs.agent/secondary-skillset []}}
      new-job-1 {::specs.events/new-job {::specs.job/id     "f26e890b-df8e-422e-a39c-7762aa0bac36",
                                         ::specs.job/type   "rewards-question",
                                         ::specs.job/urgent false}}
      new-job-2 {::specs.events/new-job {::specs.job/id     "c0033410-981c-428a-954a-35dec05ef1d2",
                                         ::specs.job/type   "bills-questions",
                                         ::specs.job/urgent true}}
      job-request {::specs.events/job-request {::specs.job-request/agent-id "8ab86c18-3fae-4804-bfd9-c3d6e8f66260"}}
      job-assigned {::specs.job-assigned/job-assigned {::specs.job-assigned/job-id   "c0033410-981c-428a-954a-35dec05ef1d2",
                                                       ::specs.job-assigned/agent-id "8ab86c18-3fae-4804-bfd9-c3d6e8f66260"}}]
  (facts "-main"
         (fact "receives a sample-input file and returns a sample-output file"
               (fix/reset-job-queues!)
               (Thread/sleep 10000)
               (-main "resources/sample-input.json.txt")
               (slurp "jobs-assigned.json.txt") => (slurp "resources/sample-output.json.txt")))
  (facts "if added-bulk-events receives an empty vector of events"
         (let [job-queues (fix/sample-job-queues)]
           (added-bulk-events [] job-queues init/default-opts)
           (fact "adds nothing to agents queue"
                 (-> job-queues ::specs.job-queues/agents deref) => [])
           (fact "adds nothing to job-requests-waiting queue"
                 (-> job-queues ::specs.job-queues/job-requests-waiting deref) => [])
           (fact "adds nothing to jobs-assigned queue"
                 (-> job-queues ::specs.job-queues/jobs-assigned deref) => [])
           (fact "adds nothing to jobs-waiting queue"
                 (-> job-queues ::specs.job-queues/jobs-waiting deref) => [])))
  (facts "if added-bulk-events only receives a new agent event"
         (let [job-queues (fix/sample-job-queues)]
           (added-bulk-events [new-agent-1] job-queues init/default-opts)
           (fact "adds the agent to agents queue"
                 (-> job-queues ::specs.job-queues/agents deref) => (contains (::specs.events/new-agent new-agent-1)))
           (fact "adds nothing to job-requests-waiting queue"
                 (-> job-queues ::specs.job-queues/job-requests-waiting deref) => [])
           (fact "adds nothing to jobs-assigned queue"
                 (-> job-queues ::specs.job-queues/jobs-assigned deref) => [])
           (fact "adds nothing to jobs-waiting queue"
                 (-> job-queues ::specs.job-queues/jobs-waiting deref) => [])))
  (facts "if added-bulk-events only receives a new job event"
         (let [job-queues (fix/sample-job-queues)]
           (added-bulk-events [new-job-1] job-queues init/default-opts)
           (fact "adds nothing to agents queue"
                 (-> job-queues ::specs.job-queues/agents deref) => [])
           (fact "adds nothing to job-requests-waiting queue"
                 (-> job-queues ::specs.job-queues/job-requests-waiting deref) => [])
           (fact "adds nothing to jobs-assigned queue"
                 (-> job-queues ::specs.job-queues/jobs-assigned deref) => [])
           (fact "adds the job to jobs-waiting queue"
                 (-> job-queues ::specs.job-queues/jobs-waiting deref) => (contains (::specs.events/new-job new-job-1)))))
  (facts "if added-bulk-events only receives a compatible pair of a new agent and a new job but NO job-request"
         (let [job-queues (fix/sample-job-queues)]
           (added-bulk-events [new-agent-1 new-job-1] job-queues init/default-opts)
           (fact "adds the agent to agents queue"
                 (-> job-queues ::specs.job-queues/agents deref) => (contains (::specs.events/new-agent new-agent-1)))
           (fact "adds nothing to job-requests-waiting queue"
                 (-> job-queues ::specs.job-queues/job-requests-waiting deref) => [])
           (fact "adds nothing to jobs-assigned queue"
                 (-> job-queues ::specs.job-queues/jobs-assigned deref) => [])
           (fact "adds the job to jobs-waiting queue"
                 (-> job-queues ::specs.job-queues/jobs-waiting deref) => (contains (::specs.events/new-job new-job-1)))))
  (facts "if added-bulk-events receives compatible new agent, new job and job request"
         (let [job-queues (fix/sample-job-queues)
               options (assoc init/default-opts :log true)]
           (added-bulk-events [new-agent-1 new-job-2 job-request] job-queues options)
           (Thread/sleep 10000)                              ;; TODO: find a better solution for this problem
           (fact "adds new agent to agents queue"
                 (-> job-queues ::specs.job-queues/agents deref) => (contains (::specs.events/new-agent new-agent-1)))
           (fact "adds NOTHING to job-request-waiting queue"
                 (-> job-queues ::specs.job-queues/job-requests-waiting deref) => [])
           (fact "adds a job-assigned to jobs-assigned queue"
                 (-> job-queues ::specs.job-queues/jobs-assigned deref) => (contains job-assigned))
           (fact "adds nothing to jobs-waiting queue"
                 (-> job-queues ::specs.job-queues/jobs-waiting deref) => [])))
  (facts "if dequeue receives in this order, a compatible new agent, two new jobs and a job-request"
         (let [job-queues (fix/sample-job-queues)]
           (added-bulk-events [new-agent-1 new-job-1 new-job-2 job-request] job-queues init/default-opts)
           (fact "adds the new agent to agents queue"
                 (-> job-queues ::specs.job-queues/agents deref) => (contains (::specs.events/new-agent new-agent-1)))
           (fact "adds NOTHING to job-requests waiting"
                 (-> job-queues ::specs.job-queues/job-requests-waiting deref) => [])
           (fact "adds job-assigned to jobs-assigned queue"
                 (-> job-queues ::specs.job-queues/jobs-assigned deref) => (contains job-assigned))
           (fact "adds the lowest priority job to jobs-waiting"
                 (-> job-queues ::specs.job-queues/jobs-waiting deref) => (contains (::specs.events/new-job new-job-1))))))

(defspec runs-with-out-erros-for-all-inputs
         100
         (prop/for-all [events (fix/gen-matching-events)]
                       (let [job-queues (fix/sample-job-queues)]
                         (added-bulk-events events job-queues init/default-opts)
                         true)))
(defspec outputs-clj-formatted-job-assigned-agent-id-and-job-id
         100
         (prop/for-all [events (fix/gen-matching-events)]
                       (let [job-queues (fix/sample-job-queues)
                             jobs-assigned (::specs.job-queues/jobs-assigned job-queues)]
                         (added-bulk-events events job-queues init/default-opts)
                         (every? #(and (= ((comp first keys) %) ::specs.job-assigned/job-assigned)
                                       (s/valid? ::specs.job-assigned/job-assigned ((comp first vals) %)))
                                 @jobs-assigned))))
(defspec job-requests>=jobs-assigned
         100
         (prop/for-all [events (fix/gen-matching-events)]
                       (let [job-queues (fix/sample-job-queues)
                             jobs-assigned (::specs.job-queues/jobs-assigned job-queues)
                             num-job-requests (reduce #(if (s/valid? ::specs.events/job-request-event %2)
                                                         (inc %1)
                                                         %1)
                                                      0 events)]
                         (added-bulk-events events job-queues init/default-opts)
                         (>= num-job-requests (count @jobs-assigned)))))
(defspec jobs>=jobs-assigned
         100
         (prop/for-all [events (fix/gen-matching-events)]
                       (let [job-queues (fix/sample-job-queues)
                             jobs-assigned (::specs.job-queues/jobs-assigned job-queues)
                             num-jobs (reduce #(if (s/valid? ::specs.events/new-job-event %2)
                                                 (inc %1)
                                                 %)
                                              0 events)]
                         (added-bulk-events events job-queues init/default-opts)
                         (>= num-jobs (count @jobs-assigned)))))
(stest/instrument)

;; TODO: implement error handling tests