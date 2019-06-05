(ns queues.models.events
  (:require [clojure.spec.alpha :as s]
            [queues.models.job :as job]
            [queues.models.agent :as agent]))

(s/def ::new-agent ::agent/agent)
(s/def ::new-job ::job/job)
(s/def ::job-request ::agent/id)

(s/def ::new-agent-event (s/keys :req [::new-agent]))
(s/def ::new-job-event (s/keys :req [::new-job]))
(s/def ::job-request-event (s/keys :req [::job-request]))

(s/def ::events (s/coll-of (s/or :new-agent ::new-agent-event
                                 :new-job ::new-job-event
                                 :job-request ::job-request-event)
                           :distinct true
                           :into []))