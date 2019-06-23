(ns queues.models.events
  (:require [clojure.spec.alpha :as s]
            [queues.models.job :as job]
            [queues.models.agent :as agent]
            [queues.models.job-request :as jr]))

(s/def ::type keyword?)

(defmulti event-type ::type)

(defmethod event-type ::agent/agent [_]
  (s/keys :req [::type ::agent/id ::agent/name ::agent/primary-skillset ::agent/secondary-skillset]))

(defmethod event-type ::job/job [_]
  (s/keys :req [::type ::job/id ::job/type ::job/urgent]))

(defmethod event-type ::jr/job-request [_]
  (s/keys :req [::type ::agent/id]))

(s/def ::event (s/multi-spec event-type ::type))

(defmethod event-type ::job-assigned [_]
  (s/keys :req [::type ::agent/id ::job/id]))

(s/def ::events (s/coll-of ::event
                           :distinct true
                           :into []))