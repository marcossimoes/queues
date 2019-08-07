(ns queues.specs.events
  (:require [clojure.spec.alpha :as s]
            [queues.specs.agent :as specs.agent]
            [queues.specs.job :as specs.job]
            [queues.specs.job-request :as specs.job-request]))

(s/def ::new-agent ::specs.agent/agent)
(s/def ::new-job ::specs.job/job)
(s/def ::job-request ::specs.job-request/job-request)

(s/def ::new-agent-event (s/keys :req [::new-agent]))
(s/def ::new-job-event (s/keys :req [::new-job]))
(s/def ::job-request-event (s/keys :req [::job-request]))

(s/def ::event (s/or :new-agent-event ::new-agent-event
                     :new-job-event ::new-job-event
                     :job-request-event ::job-request-event))

(s/def ::events (s/coll-of ::event
                           ;;:distinct true
                           :into []))
