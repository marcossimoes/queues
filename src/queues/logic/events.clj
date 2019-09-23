(ns queues.logic.events
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [queues.specs.agents :as specs.agents]
            [queues.specs.events :as specs.events]
            [queues.specs.job-request :as specs.job-req]))

;; Although this namespace has only one function, it still
;; makes sense to separate this as an architectural design
;; as to maintain all the spec handling functions in logic and not controller
;; this way the controller can remain resilient to any changes in specs

(defn event-type
  [event]
  (-> event
      keys
      first))
;;TODO [QUESTION] should this logic be hardcoded here?

(s/fdef event-type
        :args (s/cat :event ::specs.events/event)
        :ret ::specs.events/input-event-type)