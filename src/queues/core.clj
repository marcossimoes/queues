(ns queues.core
  (:gen-class))

(defn dequeue
  "Receives a pool map of new_agents, job_requests and new-jobs
  Returns a map containing the job assignments to different agents"
  [pool])

(defn -main
  [& args]
  (println "Hello, World!"))