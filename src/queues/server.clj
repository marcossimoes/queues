(ns queues.server
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [queues.service :as service]))

(defn start []
  (http/start (http/create-server service/service)))

(defonce server (atom nil))

(defn start-dev []
  (reset! server
          (http/start (http/create-server
                        (assoc service/service
                          ::http/join? false)))))

(defn stop-dev []
  (http/stop @server))

(defn restart []
  (stop-dev)
  (start-dev))