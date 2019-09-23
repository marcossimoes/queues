(ns queues.logic.agents
  (:require [clojure.spec.alpha :as s]
            [queues.specs.agents :as specs.agents]
            [queues.specs.job-request :as specs.job-req]))

;; Although this namespace has only one function, it still
;; makes sense to separate this as an architectural design
;; as to maintain all the spec handling functions in logic and not controller
;; this way the controller can remain resilient to any changes in specs

(defn job-req-from-agent [agent]
  (when-let [agent-id (::specs.agents/id agent)]
    {::specs.job-req/agent-id agent-id}))

(s/fdef job-req-from-agent
        :args (s/cat :agent ::specs.agents/agent)
        :ret ::specs.job-req/job-request)

;; TODO [QUESTION; ARCH] does it make sense the architecture decision and to keep this namespaces with only one fucntion