(ns queues.specs.agent
  (:require [clojure.spec.alpha :as s]
            [queues.specs.general :as specs.general]
            [queues.specs.job :as specs.job]))

(s/def ::id ::specs.general/id)
(s/def ::name string?)
(s/def ::primary-skillset (s/coll-of ::specs.general/skill :into [] :count 1))
(s/def ::secondary-skillset (s/coll-of ::specs.general/skill :into [] :max-count 1))
(s/def ::status #{"busy" "waiting"})
(s/def ::jobs-being-done (s/coll-of ::specs.job/job
                                    ;;:distinct true
                                    :into []))
(s/def ::jobs-done (s/coll-of ::specs.job/job
                                    ;;:distinct true
                                    :into []))

(s/def ::agent (s/keys :req [::id
                             ::name
                             ::primary-skillset
                             ::secondary-skillset]
                       :opt [::status
                             ::jobs-being-done
                             ::jobs-done]))