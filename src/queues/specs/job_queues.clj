(ns queues.specs.job-queues
  (:require [clojure.spec.alpha :as s]
            [queues.specs.agent :as specs.agent]
            [queues.specs.job :as specs.job]
            [queues.specs.job-assigned :as specs.job-assigned]
            [queues.specs.job-request :as specs.job-request]
            [queues.specs.priority-rule :as specs.priority-rule]))

(s/def ::agents (s/map-of ::specs.agent/id ::specs.agent/agent))

(s/def ::jobs-waiting (s/coll-of ::specs.job/job
                                 ;;:distinct true
                                 :into []))

(s/def ::job-reqs (s/coll-of ::specs.job-request/job-req
                             ;;:distinct true
                             :into []))

(s/def ::jobs-assigned (s/coll-of (s/keys :req [::specs.job-assigned/job-assigned])
                                  ;;:distinct true
                                  :into []))

(s/def ::priority-rules (s/coll-of ::specs.priority-rule/priority-rule
                                   :distinct true
                                   :into []
                                   :min-count 1
                                   :max-count 4))

(s/def ::job-queues (s/keys :req [::agents
                                  ::job-reqs
                                  ::jobs
                                  ::priority-rules]))