(ns queues.logic.agents
  (:require [clojure.spec.alpha :as s]
            [queues.specs.agent :as specs.agent]
            [queues.specs.job-queues :as specs.job-queues]
            [queues.specs.job-request :as specs.job-request]))

(defn agent-found
  "Receives job-queues and a job-request payload and returns the agent related
  to that job request or nil if no agent is found"
  [job-queues job-req-payload]
  (let [agent-id ((comp first vals) job-req-payload)
        agents (::specs.job-queues/agents job-queues)]
    (get @agents agent-id)))

;;(s/fdef agent-found
;;        :args (s/cat :job-queues ::specs.job-queues/job-queues
;;                     :job-req-payload ::specs.job-request/job-req)
;;        :ret (s/or :found ::specs.agent/agent
;;                   :not-found nil?)
;;        :fn (s/or :found #(= (-> % :ret :found ::specs.agent/id)
;;                             (-> % :args :job-req-payload ::specs.job-request/agent-id))
;;                  :not-found #(nil? (-> :ret :not-found %))))

(defn agent-skillsets
  "Receives an agent an returns a coll with its skillsets"
  [agent]
  (concat (::specs.agent/primary-skillset agent)
          (::specs.agent/secondary-skillset agent)))

(defn agent-with-job-done
  [agent]
  (if-let [job-being-done (::specs.agent/job-being-done agent)]
    (update agent ::specs.agent/jobs-done conj job-being-done)
    agent))

(defn update-agent-in-agents
  [agents agent]
  (let [agent-id (::specs.agent/id agent)]
    (assoc agents agent-id agent)))

(defn job-queues-with-updated-agent
  [new-agent job-queues]
  (-> job-queues
      ::specs.job-queues/agents
      (send update-agent-in-agents new-agent)))

(defn agent-in-job-queues-with-status
  ([job-queues job-req-payload status]
    (agent-in-job-queues-with-status job-queues job-req-payload status nil))
  ([job-queues job-req-payload status job]
   (-> job-queues
       (agent-found job-req-payload)
       (agent-with-job-done)
       (assoc ::specs.agent/job-being-done job)
       (assoc ::specs.agent/status status)
       (job-queues-with-updated-agent job-queues))))

;; FIXME: when new-agent is entered check for corresponding job-requestand new-jobs
;; FIXME: when an agent id or job id is entered for the second time, update the original agent/id
;; TODO: implement the alternative of multiple skills in the primary or secondary skillset vectors