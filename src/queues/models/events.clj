(ns queues.models.events
  (:require [clojure.spec.alpha :as s]
            [queues.models.job :as job]
            [queues.models.agent :as agent]
            [queues.models.job-request :as jr]))

(s/def ::new-agent ::agent/agent)
(s/def ::new-job ::job/job)
(s/def ::job-request ::jr/job-request)

(s/def ::new-agent-event (s/keys :req [::new-agent]))
(s/def ::new-job-event (s/keys :req [::new-job]))
(s/def ::job-request-event (s/keys :req [::job-request]))

(s/def ::event (s/or :new-agent-event ::new-agent-event
                     :new-job-event ::new-job-event
                     :job-request-event ::job-request-event))

(s/def ::events (s/coll-of ::event
                           :distinct true
                           :into []))