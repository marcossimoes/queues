(ns queues.specs.events
  (:require [clojure.spec.alpha :as s]
            [queues.specs.agents :as specs.agents]
            [queues.specs.job :as specs.job]
            [queues.specs.job-assigned :as specs.job-assigned]
            [queues.specs.job-request :as specs.job-req]))

(s/def ::new-agent ::specs.agents/agent)
(s/def ::new-job ::specs.job/job)
(s/def ::job-request ::specs.job-req/job-request)

(s/def ::new-agent-event (s/keys :req [::new-agent]))
(s/def ::new-job-event (s/keys :req [::new-job]))
(s/def ::job-request-event (s/keys :req [::job-request]))

(s/def ::input-event-type #{"agent" "job" "job-request"})

(s/def ::event (s/or :new-agent ::new-agent-event
                     :new-job ::new-job-event
                     :job-request ::job-request-event))

(s/def ::events (s/coll-of ::event
                           ;;:distinct true
                           :into []))

;; TODO [READ] rewrite events with {event-type event} structure