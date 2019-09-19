(ns queues.specs.order-of-priority
  (:require [clojure.spec.alpha :as s]
            [queues.specs.matching-criteria :as specs.matching-criteria]))

(s/def ::order-of-priority (s/coll-of ::specs.matching-criteria/matching-criteria
                                      :distinct true
                                      :into []
                                      :min-count 1
                                      :max-count 4))