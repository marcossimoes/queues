(ns queues.test
  (:require [clojure.test :refer :all]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s]
            [queues.models.agent :as agent]
            [queues.models.job :as job]
            [queues.models.events :as events]))

(def agent-ids (gen/sample (gen/not-empty (s/gen ::agent/id))))
(def job-ids (gen/sample (gen/not-empty (s/gen ::job/id))))
(def job-types (gen/sample (gen/not-empty (s/gen ::job/type))))

(def overrides
  {::agent/id #(gen/elements agent-ids)
   ::job/id   #(gen/elements job-ids)
   ::job/type #(gen/elements job-types)})

(defn gen-events
  "generates a random vector of events with common
  - agent-ids
  - job-ids
  - job-types"
  [] (gen/vector (s/gen ::events/event overrides) 0 100))