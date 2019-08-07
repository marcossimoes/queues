(ns queues.events-test
  (:require [midje.sweet :refer :all]
            [clojure.spec.test.alpha :as stest]
            [clojure.test.check.clojure-test :refer [defspec]]
            [queues.init :as init]
            [queues.logic.events :refer :all]
            [queues.specs.agent :as specs.agent]
            [queues.specs.events :as specs.events]
            [queues.specs.job :as specs.job]
            [queues.specs.job-request :as specs.job-request]
            [queues.specs.job-assigned :as specs.job-assigned]
            [queues.specs.job-queues :as specs.job-queues]))

(let [job-request-content {::specs.job-request/agent-id "8ab86c18-3fae-4804-bfd9-c3d6e8f66260"}
      job-content-1 {::specs.job/id     "f26e890b-df8e-422e-a39c-7762aa0bac36",
                     ::specs.job/type   "rewards-question",
                     ::specs.job/urgent false}
      job-content-2 {::specs.job/id     "c0033410-981c-428a-954a-35dec05ef1d2",
                     ::specs.job/type   "bills-questions",
                     ::specs.job/urgent true}
      job-assigned {::specs.job-assigned/job-assigned
                    {::specs.job-assigned/job-id   "c0033410-981c-428a-954a-35dec05ef1d2",
                     ::specs.job-assigned/agent-id "8ab86c18-3fae-4804-bfd9-c3d6e8f66260"}}
      jqs-with-new-agent (assoc init/job-queues ::specs.job-queues/agents (agent [{::specs.agent/id                 "8ab86c18-3fae-4804-bfd9-c3d6e8f66260",
                                                                                   ::specs.agent/name               "BoJack Horseman",
                                                                                   ::specs.agent/primary-skillset   ["bills-questions"],
                                                                                   ::specs.agent/secondary-skillset []}]))
      jqs-with-new-job-1 (assoc jqs-with-new-agent ::specs.job-queues/jobs-waiting (ref [job-content-1]))
      jqs-with-new-job-2 (assoc jqs-with-new-agent ::specs.job-queues/jobs-waiting (ref [job-content-2]))]
  (facts "dequeue"
         (fact "if no job is available for the agent returns job request queued in job-requests-waiting"
               (dequeue init/job-queues job-request-content)
               (-> init/job-queues
                   ::specs.job-queues/job-requests-waiting
                   deref)
               => (contains job-request-content)
               (dequeue jqs-with-new-job-1 job-request-content)
               (-> init/job-queues
                   ::specs.job-queues/job-requests-waiting
                   deref)
               => (contains job-request-content))
         (fact "if a job is available for the agent returns job request assigned"
               (dequeue jqs-with-new-job-2 job-request-content)
               (-> init/job-queues
                   ::specs.job-queues/jobs-assigned
                   deref)
               => (contains job-assigned))))

(let [new-agent-1 {::specs.events/new-agent {::specs.agent/id                 "8ab86c18-3fae-4804-bfd9-c3d6e8f66260",
                                             ::specs.agent/name               "BoJack Horseman",
                                             ::specs.agent/primary-skillset   ["bills-questions"],
                                             ::specs.agent/secondary-skillset []}}
      new-job-1 {::specs.events/new-job {::specs.job/id     "f26e890b-df8e-422e-a39c-7762aa0bac36",
                                         ::specs.job/type   "rewards-question",
                                         ::specs.job/urgent false}}
      agent #:queues.specs.agent{:id                 "8ab86c18-3fae-4804-bfd9-c3d6e8f66260",
                                 :name               "BoJack Horseman",
                                 :primary-skillset   ["bills-questions"],
                                 :secondary-skillset []}
      job #:queues.specs.job{:id     "f26e890b-df8e-422e-a39c-7762aa0bac36",
                             :type   "rewards-question",
                             :urgent false}]
  (facts "added-event"
         (fact "Adds new agents and new jobs to their respective queues in agents and jobs"
               (added-event init/job-queues new-agent-1)
               (-> init/job-queues
                   ::specs.job-queues/agents
                   deref)
               => (contains [agent])
               (added-event init/job-queues new-job-1)
               (-> init/job-queues
                   ::specs.job-queues/jobs-waiting
                   deref)
               => (contains [job]))))

;; TODO: create test for processed-new-job

(stest/instrument)