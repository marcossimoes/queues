(ns queues.specs.json-events
  (:require [clojure.spec.alpha :as s]
            [queues.specs.agents :as specs.agents]
            [queues.specs.job :as specs.job]
            [queues.specs.job-assigned :as specs.job-assigned]
            [queues.specs.job-request :as specs.job-req]))



(s/def ::new-agent (s/map-of string? (s/or :val any?
                                           :vec (s/coll-of string?))))
(s/def ::new-job (s/map-of string? any?))
(s/def ::job-request (s/map-of string? any?))

(s/def ::new-agent-map (s/map-of string? ::new-agent))
(s/def ::new-job-map (s/map-of string? ::new-job))
(s/def ::job-request-map (s/map-of string? ::job-request))

(s/def ::json-event (s/or :new_agent ::new-agent-map
                     :new_job ::new-job-map
                     :job_request ::job-request-map))

(s/def ::json-events (s/coll-of ::json-event
                                :into []))

