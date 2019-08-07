(ns queues.app
  (:require [cheshire.core :refer :all]
            [compojure.core :refer [defroutes ANY]]
            [liberator.core :refer [resource defresource]]
            [ring.middleware.params :refer [wrap-params]]
            [queues.logic.events :as logic.events]
            [queues.logic.jobs :as logic.jobs]
            [queues.init :as init]
            [queues.io :as io]
            [queues.specs.agent :as specs.agent]
            [queues.specs.events :as specs.events]
            [queues.specs.job-assigned :as specs.job-assigned]))

(defn body-as-string
  [ctx]
  (if-let [body (get-in ctx [:request :body])]
    (instance? String body)))

(defn parse-json
  [ctx key]
  (when (#{:put :post} (get-in ctx [:request :request-method]))
    (try
      (if-let [body (body-as-string ctx)]
        (let [event (-> body
                       parse-string
                       io/read-json-event)]
          [false {key event}])
        {:message "No body"})
      (catch Exception e
        (.printStackTrace e)
        {:message (format "IOException: %s" (.getMessage e))}))))

(defn check-content-type
  [ctx content-types]
  (if (#{:put :post} (get-in ctx [:request :request-method]))
    (or
      (some #{(get-in ctx [:request :headers "content-type"])}
            content-types)
      [false {:message "Unsupported Content-Type"}])
    true))

(defn post-job
  [id])

(defn post-job-request
  [agent-id])

(defn post-agent
  [id])

(defn get-agent
  [id])

(defn get-job-queues
  [])

(defroutes app
           (ANY "/" [] (resource
                         :allowed-methods [:get]
                         :handle-ok (get-job-queues)))
           (ANY "/agents/:id" [id] (resource
                                     :allowed-methods [:get]
                                     :exists? (when-let [agent (get-agent (str id))]
                                                {:agent agent})
                                     :handle-ok :agent))
           (ANY "/job-request" [] (resource
                                    :available-media-types ["application/json"]
                                    :allowed-methods [:put :post]
                                    :known-content-type? #(check-content-type % ["application/json"])
                                    :malformed? #(parse-json % ::event)
                                    :post! #(logic.events/dequeue init/job-queues (::event %))
                                    :handle-ok #(cond
                                                  (::specs.agent/agent %) (::specs.agent/agent %)
                                                  (::specs.job-assigned/job-assigned %) (::specs.job-assigned/job-assigned %))))
           ;;(ANY "/agents" [id] (post-agent id))
           ;;(ANY "/jobs" [id] (post-job id))
           )

(def handler
  (-> app
      wrap-params))

;; TODO: research pedestal or compojure