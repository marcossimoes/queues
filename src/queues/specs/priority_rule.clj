(ns queues.specs.priority-rule
  (:require [clojure.spec.alpha :as s]
            [queues.specs.agent :as specs.agent]
            [queues.specs.job :as specs.job]))

(s/def ::skill-type #{::specs.agent/primary-skillset ::specs.agent/secondary-skillset})
(s/def ::urgent ::specs.job/urgent)
(s/def ::priority-rule (s/keys :req [::skill-type
                                     ::urgent]))