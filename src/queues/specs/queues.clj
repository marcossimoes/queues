(ns queues.specs.queues
  (:require [clojure.spec.alpha :as s]
            [queues.specs.agents :as specs.agents]
            [queues.specs.job :as specs.job]
            [queues.specs.job-request :as specs.job-request]))

(s/def ::job-queue (s/coll-of ::specs.job/job
                              ;;:distinct true
                              :into []))

;;TODO [QUESTION; ALGORITHM] how to implement job-id uniqueness in job-queue spec?
;; solution above generates job-queue with only one job
;;(s/and (s/coll-of ::specs.job/job :distinct true :into [])
;;       #(when (not-empty %) (apply distinct? (map :id %))))

(s/def ::jobs-waiting ::job-queue)
(s/def ::jobs-in-progress ::job-queue)
(s/def ::jobs-done ::job-queue)

(s/def ::job-reqs-queued (s/coll-of ::specs.job-request/job-request
                                    :into []))

(s/def ::agents-waiting-for-job (s/coll-of ::specs.agents/agent
                                           ;;:distinct true
                                           :into []))