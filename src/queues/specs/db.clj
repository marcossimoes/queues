(ns queues.specs.db
  (:require [clojure.spec.alpha :as s]
            [queues.specs.order-of-priority :as specs.order-of-priority]))

;; Clojure Spec does not allow to specify object types like refs and agents as part of the specs
;; nevertheless we want to make sure that the db map with objects passed to the functions adheres
;; to the minimum spec of a map contain the right keys.

(s/def ::agents any?)
(s/def ::jobs-waiting any?)
(s/def ::jobs-in-progress any?)
(s/def ::jobs-done any?)
(s/def ::job-reqs-queued any?)
(s/def ::order-of-priority ::specs.order-of-priority/order-of-priority)

(s/def ::db (s/keys :req [::agents
                          ::jobs-waiting
                          ::jobs-in-progress
                          ::jobs-done
                          ::job-reqs-queued
                          ::order-of-priority]))