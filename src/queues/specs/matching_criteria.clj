(ns queues.specs.matching-criteria
  (:require [clojure.spec.alpha :as s]
            [queues.specs.agents :as specs.agents]
            [queues.specs.job :as specs.job]))

;; FIXME: the alternative commented is not working
;;(s/def ::skillset (set specs.agents/skillset-types-in-order-of-priority))
(s/def ::skillset #{:queues.specs.agents/primary-skillset ::specs.agents/secondary-skillset})
(s/def ::urgent ::specs.job/urgent)
(s/def ::matching-criteria (s/keys :req [::skillset
                                         ::urgent]))