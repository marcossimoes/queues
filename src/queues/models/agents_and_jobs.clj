(ns queues.models.agents-and-jobs
  (:require [clojure.spec.alpha :as s]
            [queues.models.job :as job]
            [queues.models.agent :as agent]))

(s/def ::agents (s/coll-of ::agent/agent :distinct true :into []))
(s/def ::jobs-assigned (s/coll-of ::job/job :distinct true :into []))
(s/def ::jobs-waiting (s/coll-of ::job/job :distinct true :into []))

(s/def ::agents-and-jobs (s/keys :req [::agents ::jobs-assigned ::jobs-waiting]))
