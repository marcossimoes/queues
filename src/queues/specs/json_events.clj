(ns queues.specs.json-events
  (:require [clojure.spec.alpha :as s]
            [queues.specs.agents :as specs.agents]
            [queues.specs.job :as specs.job]
            [queues.specs.job-assigned :as specs.job-assigned]
            [queues.specs.job-request :as specs.job-req]))

(s/def ::json-key keyword?)

(s/def ::new-agent (s/map-of ::json-key (s/or :val any?
                                           :vec (s/coll-of string?))))
(s/def ::new-job (s/map-of ::json-key any?))
(s/def ::job-request (s/map-of ::json-key any?))

(s/def ::new-agent-map (s/map-of ::json-key ::new-agent))
(s/def ::new-job-map (s/map-of ::json-key ::new-job))
(s/def ::job-request-map (s/map-of ::json-key ::job-request))

(s/def ::json-event (s/or :new_agent ::new-agent-map
                          :new_job ::new-job-map
                          :job_request ::job-request-map))

(s/def ::json-events (s/coll-of ::json-event
                                :into []))

