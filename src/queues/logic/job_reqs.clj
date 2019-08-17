(ns queues.logic.job-reqs
  (:require [clojure.spec.gen.alpha]
            [clojure.spec.alpha :as s]
            [queues.logic.agents :as agents]
            [queues.specs.agent :as specs.agent]
            [queues.specs.job :as specs.job]
            [queues.specs.job-queues :as specs.job-queues]
            [queues.specs.job-request :as specs.job-request]))

(defn matching-waiting-job-request
  "Receives an 'job-queues' map and a job
  and returns a matching job request or nil if non exists"
  [job-queues job-payload]
  (let [job-requests-waiting (::specs.job-queues/job-requests-waiting job-queues)]
    (some (fn [job-request]
            (->> job-request
                 (agents/agent-found job-queues)
                 (agents/agent-skillsets)
                 (some #{(::specs.job/type job-payload)})
                 (#(if % job-request))))
          @job-requests-waiting)))

;;(s/fdef matching-waiting-job-req
;;        :args (s/cat :job-queues ::specs.job-queues/job-queues
;;                     :job-payload ::specs.job/job)
;;        :ret (s/or :no-job-requestnil?
;;                   :job-req-found ::specs.job-request/job-req))

(defn queued-job-request
  "Receives job-queues and a job request payload and returns
  job-queues with a job request "
  [job-queues job-req-payload]
  (-> job-queues
      ::specs.job-queues/job-requests-waiting
      (alter conj job-req-payload))
  (->> job-req-payload
       (queues.logic.agents/agent-found job-queues)
       (#(assoc % ::specs.agent/status "waiting"))
       (hash-map ::specs.agent/agent)))

;;(s/fdef queued-job-request
;;        :args (s/cat :job-queues ::specs.job-queues/job-queues
;;                     :job-req-payload ::specs.job-request/job-req)
;;        :ret ::specs.job-queues/job-queues
;;        :fn (s/and #(= (-> % :ret ::specs.job-queues/job-reqs-waiting drop-last)
;;                       (-> % :args :job-queues ::specs.job-queues/job-reqs-waiting))
;;                   #(= (-> % :ret ::specs.job-queues/job-reqs-waiting last)
;;                       (-> % :args :job-req-payload))))

;; TOOD: separate controller, logic and stateful functions (arquitetura hexagonal ou ports and adapters)