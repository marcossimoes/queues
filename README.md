# Job Queue

## Brief Description

This app was developed as an answer to Nubank's queues 1 exercise.
The exercise's rubcric can be found [here](./RUBRIC.md)

THe app takes a text file with a json formatted list of customer service events,
namely a **new_agent** that has certain skillsets, a **job** that is from a certain type
(respective to the agents skillsets) and a **job-request** from one of the agents
stating the agent is ready to take a new job.

The app should then return a text file with a json formatted list of jobs-assigned,
each one containing the agent-id that received the job-assignment and the job-id from
the job that was assigned to the agent.

Given agents have a primary and a secondary skillset, and jobs have an urgent flag (true or false),
jobs will be assigned to Agents accordingly to the following rules
1. The **first** Job that came and matches the agent's **Primary *Urgent* **
2. The **first** Job that came and matches the agent's **Primary*
3. The **first** Job that came and matches the agent's **Secondary *Urgent* **
3. The **first** Job that came and matches the agent's **Secondary**

## Usage

### System requirements

To make sure you are able to build and run the application make sure you have Java 8 or higher
(recommended: Java 8 or Java 11) and Leiningen installed and reachable in your path.

### Build and Execution

Inside queues project directory, just type

```
lein uberjar
```

To start the application, just execute

1. if you want to specify your own input file

```
    $ java -jar queues-0.1.0-standalone.jar YOUR_INPUT_FILE_PATH
```
2. if you want to run the program with the sample-input-file
```
    $ java -jar queues-0.1.0-standalone.jar
```

By default the program will generate a file names **jobs-assinged.json.txt** in the root directory of the project

## Options

It is possible to change some behaviours of the application by passing a parameter in it'' initialization call.

| Option               | Description                                          |
|----------------------|------------------------------------------------------|
|-l, --log             | Prints to termainal logs of the apps processing steps|
|-p, --pretty-print    | Turn on pretty print to Terminal of the json output  |
|-f, --output-file     | Let specify the name of the output file.<br>Defaults to "jobs-assigned.json.txt" if nor provided |

## Examples

There is a sample input file you can run for an example

```
    $ java -jar queues-0.1.0-standalone.jar resources/sample-input.json.txt
```

## Testing

The test suit contains a combination of unit tests, property based tests, as well as Clojure Spec `s/fdef` definitions
that are enabled via instrumentation when running the tests.

The example based unit tests verify happy path and corner cases for specific functions
in a way to quickly identify problems in refactoring. It also verifies the program produces a file equal in content to
sample-output.json.txt after processing sample-input.json.txt

The property based tests verify that requirements of the job queue hold true, after applying randomly generated
sequence of events to the job queue.

The test suite can be run with the following command

```
lein midje
```

```
{:result true, :num-tests 100, :seed 1562682111388, :time-elapsed-ms 782, :test-var "outputs-clj-formatted-job-assigned-agent-id-and-job-id"}
{:result true, :num-tests 100, :seed 1562682112171, :time-elapsed-ms 573, :test-var "jobs>=jobs-assigned"}
{:result true, :num-tests 100, :seed 1562682112745, :time-elapsed-ms 672, :test-var "job-requests>=jobs-assigned"}
{:result true, :num-tests 100, :seed 1562682113418, :time-elapsed-ms 573, :test-var "runs-with-out-erros-for-all-inputs"}

>>> Midje summary:
All checks (73) succeeded.

>>> Output from clojure.test tests:

Ran 4 tests containing 4 assertions.
0 failures, 0 errors.
```

### Coverage

This is the current test coverage of the app in accordance to clojure cloverage specification

|-------------------------------|---------|---------|
|                     Namespace | % Forms | % Lines |
|-------------------------------|---------|---------|
|                   queues.core |  100,00 |  100,00 |
|                   queues.json |   97,46 |   97,62 |
|           queues.models.agent |   85,19 |  100,00 |
| queues.models.agents-and-jobs |   52,77 |  100,00 |
|          queues.models.events |   81,33 |  100,00 |
|             queues.models.job |   80,65 |  100,00 |
|    queues.models.job-assigned |   53,85 |  100,00 |
|     queues.models.job-request |   85,96 |  100,00 |
|-------------------------------|---------|---------|
|                     ALL FILES |   80,04 |   99,44 |
|-------------------------------|---------|---------|


## Implementation

The job queue is implemented as a reduction of a sequence of events over an immutable, in-memory data structure.

The project was divided in 2 (two) logic namespaces + 6 (six) specs namespaces
  * **queues.core** - It is the application startup point. It is where the -main function is declared and where all the main
  business logic for processing the events is stored.
  * **queues.json** - it has functions to deal with json, including reading a json from stdin and writing to stdout
  * **queues.models.[spec-name]** - holds clojure spec for all the main data structures used in the app, namely
      * **agents-and-jobs** - map holding seqs and queues such as: agents, jobs-waiting-to-be-assigned,
      job-requests-waiting-to-be-fullfilled, jobs-assigned
      * **events** - seq of events, each event being either a new_agent to be included in agent seq in agents-and-jobs.
      a new_job to be either assigned or included in jobs-waiting queue in agents-and-jobs and job_request to be either
      assigned or included in jobs-waiting queue in agents-and-jobs.
      * **agent** - and agent with id, name, primary and secondary skillsets
      * **job** - with id, type and urgent flag
      * **job-assigned** - with job-id and agent-id
      * **job-request** with and agent-id

The application was coded in a way that all the functions containing side effects are separated in the boarders of the
application. They are used mainly to read json from stdin and write json to the stdout and called directly by the main
function. The agents-and-jobs queues are entirely managed by interactions between functions as accordingly to the
prerequisites of the exercise itself there is no need to store state in a persistent way. The application starts,
receives an input, processes it, and prints an output to the stdout. Thus almost all functions in the whole application
are pure functions.

### Processing

A sequence of events can be processed into agents-and-jobs queues by using the `dequeue` function. The function returns
a list of jobs-assigned.

```
   (-> input-file
      (slurp)
      (json/read-json-events)
      (dequeue)
      (json/write-json-events)
      (#(spit "jobs-assigned.json.txt" %)))
```

## Planned Improvements
1. Merge all specs in one single file: some of the spec files are too short and have common domain with other.
It could be easier to go over all specs and understand there interdependence if they are in one single reasonably sized
file
2. priority queue is a configuration of the program that would be better organized if displayed in the same
initial step where the agents-and-jobs queues map is specified
3. the program now assumes that a job request is only made after an agent already exists. If an agent is added
to the agents seq, after a job request is assigned, even if there is a corresponding job waiting to be assigned
nothing will happend. This should be change so as when an agent is added, the program loos for requests and jobs waiting
that could now be assigned.
4. the program assumes that an agent and a job will never be entered twice. This should be changed. One possible
behaviour is that an agent or a job added by the second type could be considered an update on the respective structure
properties, and this should replace the previous one.
5. reading and writing to stdin and stdout are implemented via slurp and spit. This could be made safer by using edn
6. there are no error handling in place.

## Bugs
1. runtime checks with fdef are still not coded
2. logging functionality is till not coded
3. none of the option in tun are still coded

...