(ns queues.core
  (:gen-class))

(defn added-event
  "Receives a map of agents and jobs asigned and an event
  processes the event and adds the result to agents and "
  [agents-and-jobs-asigned event])

(defn dequeue
  "Receives a pool map of new_agents, job_requests and new-jobs
  Returns a map containing the job assignments to different agents"
  ([events]
   (let [agents-and-jobs {:agents {}
                          :jobs-assigned {}
                          :jobs-waiting {}}]
     (dequeue events
              agents-and-jobs)))
  ([events agents-and-jobs]
   (-> (reduce (partial added-event) agents-and-jobs events)
       (:jobs-assigned))))

(defn -main
  [& args]
  (println "Hello, World!"))