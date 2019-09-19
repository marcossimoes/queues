(ns queues.specs.event-processor
  (:require [clojure.spec.alpha :as s]
            [queues.specs.db :as specs.db]
            [queues.specs.events :as specs.events]))

(s/def ::event-processor (s/fspec :args (s/cat :db ::specs.db/db
                                               :event ::specs.events/event)))