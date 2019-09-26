(ns queues.json-converter
  (:require [cheshire.core :as che]
            [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [queues.specs.agents :as specs.agents]
            [queues.specs.job :as specs.job]
            [queues.specs.job-request :as specs.job-request]
            [queues.specs.events :as specs.events]
            [queues.specs.jobs-assigned :as specs.jobs-assigned]
            [queues.specs.json-events :as specs.json-events]))

;; TODO [IMPROVE] Handle exception when no case is found
(defn- clj-key-from-json-key
  ([js-kw]
   (case js-kw
     :new_agent ::specs.events/new-agent
     :new_job ::specs.events/new-job
     :job_request ::specs.events/job-request))
  ([event-type js-kw]
   (case [event-type js-kw]
     [:new_agent :id] ::specs.agents/id
     [:new_agent :name] ::specs.agents/name
     [:new_agent :primary_skillset] ::specs.agents/primary-skillset
     [:new_agent :secondary_skillset] ::specs.agents/secondary-skillset
     [:new_job :id] ::specs.job/id
     [:new_job :type] ::specs.job/type
     [:new_job :urgent] ::specs.job/urgent
     [:job_request :agent_id] ::specs.job-request/agent-id)))

(s/fdef clj-key-from-json-key
        :args (s/alt :unary (s/cat :js-kw ::specs.json-events/json-key)
                     :dueary (s/cat :event-type keyword?
                                    :js-kw ::specs.json-events/json-key))
        :ret keyword?)

(defn- clj-event-payload-from-json-type-and-payload
  [js-event-type js-event-payload]
  (reduce-kv
    (fn [clj-payload json-key json-value]
      (let [clj-key (clj-key-from-json-key js-event-type json-key)
            clj-value json-value]
        (assoc clj-payload clj-key clj-value)))
    {}
    js-event-payload))

(s/fdef clj-event-payload-from-json-type-and-payload
        :args (s/cat :js-event-type keyword?
                     :js-event-payload map?)
        :ret ::specs.events/event)

(defn clj-event-from-json-event
  [js-event]
  (let [[js-event-type js-event-payload] (first js-event)
        clj-event-type (clj-key-from-json-key js-event-type)
        clj-event-payload (clj-event-payload-from-json-type-and-payload js-event-type js-event-payload)]
    (->> {clj-event-type clj-event-payload}
         (s/conform ::specs.events/event)
         (second))))

(s/fdef clj-event-from-json-event
        :args (s/cat :js-event map?)
        :ret ::specs.events/event)

(defn- clj-events-with-converted-json-event
  [clj-events js-event]
  (conj clj-events (clj-event-from-json-event js-event)))

(s/fdef clj-events-with-converted-json-event
        :args (s/cat :clj-events ::specs.events/events
                     :js-event map?)
        :ret ::specs.events/events)

;; TODO [IMPROVE, TEST] have a runtime check for the js-events after the file content string has been parsed
;; we should through an exception/error and ask the user to provide a new file, or check the file
;; for corruption

(defn- clj-events-vec-from-json-events
  [json-events]
  (reduce clj-events-with-converted-json-event [] json-events))

(s/fdef clj-events-vec-from-json-events
        :args (s/cat :json-events ::specs.json-events/json-events)
        :ret ::specs.events/events)

;; TODO [IMPROVE] write spec and test for json-from-str

(defn json-from-str [str] (che/parse-string str true))

(defn json-event-from-json-event-str
  [json-event-str]
  (json-from-str json-event-str))

(s/fdef json-event-from-json-event-str
        :args (s/cat :json-event-str string?)
        :ret ::specs.json-events/json-event)

(defn clj-event-from-json-event-str
  [json-event-str]
  (-> json-event-str
      (json-event-from-json-event-str)
      (clj-event-from-json-event)))

(s/fdef clj-event-from-json-event-str
        :args (s/cat :json-event-str string?)
        :ret ::specs.events/event)

(defn- json-events-vec-from-json-events-str
  [json-events-str]
  (if-let [json-events-vec (json-from-str json-events-str)]
    json-events-vec
    []))

(s/fdef json-events-vec-from-json-events-str
        :args (s/cat :json-events-str string?)
        :ret (s/or :no-events nil?
                   :json-events ::specs.json-events/json-events))

(defn clj-events-vec-from-json-events-str
  [json-events-str]
  (when-let [json-events-vec (json-events-vec-from-json-events-str json-events-str)]
    (clj-events-vec-from-json-events json-events-vec)))

(s/fdef clj-events-vec-from-json-events-str
        :args (s/cat :json-events-str string?)
        :ret (s/or :events ::specs.events/events
                   :no-events nil?))

(defn- json-key-from-clj-ns-kwd-key
  [clj-ns-kwd-key]
  (->> clj-ns-kwd-key
       (name)
       (#(str/replace % #"-" "_"))))

(s/fdef json-key-from-clj-ns-kwd-key
        :args (s/cat :clj-ns-kwd-key keyword?)
        :ret string?)

(defn json-events-str-from-clj-events
  [clj-events]
  (che/generate-string clj-events {:key-fn json-key-from-clj-ns-kwd-key :pretty true}))

(s/fdef json-events-str-from-clj-events
        :args (s/cat :clj-events any?)
        :ret string?)

;; TODO [IMPROVE] Have two specs for events: input-events and output-events