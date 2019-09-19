(ns queues.controllers.new-event-processor-test
  (:require [midje.sweet :refer :all]
            [clojure.spec.test.alpha :as stest]
            [clojure.test.check.clojure-test :refer [defspec]]
            [queues.controllers.new-event-processor :refer :all]
            [queues.init :as init]
            [queues.specs.agents :as specs.agents]
            [queues.specs.db :as specs.db]
            [queues.specs.events :as specs.events]
            [queues.specs.job :as specs.job]
            [queues.specs.job-request :as specs.job-request]
            [queues.specs.job-assigned :as specs.job-assigned]
            [queues.test-cases :as cases]))

(facts "move-job-to-done!")
(facts "assign-agent-to-job"
       (fact "adds job to jobs in progress with agent-id added to it"
             (let [db (init/db)
                   job-1t-doing (assoc cases/job-1t ::specs.job/assigned-agent cases/agent-p1-id)]
               (dosync
                 (assign-agent-to-job db cases/job-1t cases/job-req-p1)
                 (-> db ::specs.db/jobs-in-progress deref) => (contains job-1t-doing)))))
(facts "processed-event-by-type")
(facts "processed-event-with-log!")

;; TODO [TEST] new-event-processor tests

(stest/instrument)