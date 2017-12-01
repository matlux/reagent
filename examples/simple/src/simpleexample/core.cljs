(ns simpleexample.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<! take!]]
            [ajax.core :refer [GET POST]]
            [reagent.core :as r]
            [simpleexample.components :as c]
            [simpleexample.geometry :as g])
  )

(comment

  (use 'figwheel-sidecar.repl-api)
  (start-figwheel!)
  (cljs-repl)

  (in-ns 'simpleexample.core)
  *ns*
  (println "hiya2")
  (http/get "https://api.github.com/users"
            {:with-credentials? false
             :query-params      {"since" 135}})

  (def c (go (let [response (<! (http/get "https://api.github.com/users"
                                          {:with-credentials? false
                                           :query-params      {"since" 135}}))]
               (prn (:status response))
               (prn (map :login (:body response))))))
  (take! (go (let [response (<! (http/get "https://api.github.com/users"
                                          {:with-credentials? false
                                           :query-params      {"since" 135}}))]
               (prn (:status response))
               (prn (map :login (:body response)))))
         #(println %))

  (take! c #(println %))
  (GET "http://localhost:8080/bar")
  (foo)
  @points

  (map (fn [p1 p2] [p1 p2]) '(1 2 3 4) (drop 1 '(1 2 3 4)))

  (conj [1 2 3] 4)
  (type (js->clj (get-point)))
  (type 4)

  (->>
    (reduce (fn [[acc prevpts] pt] (vector (inc acc) (conj prevpts (conj pt acc)))) [0 []] '([1 2] [2 3] [3 4]))
    second;
    rescale
    )

  (->> (map (fn [p1 p2] [p1 p2]) '(1 2 3 4 5) (drop 1 '(1 2 3 4 5)))
       (reduce (fn [[acc prevpts] pt] (vector (inc acc) (conj prevpts (conj pt acc)))) [0 []] )
       second
       (rescale 5)
       ;plot
       )
  (graph '(1 2 3 4))
  (into []  (take-last 20 [1 2 3 4]))
  (foo4)

    )

(enable-console-print!)

(defonce timer (r/atom (js/Date.)))

(def time-color (r/atom "#920"))
(def test-field (r/atom "data..."))
(def tx-time (atom 0))
(def thread-count (atom 0))

(defonce points
    (r/atom
      [{:status 503 :y 10}
       ]))

(defn rescale [max-y coll]
  (let [width 800
        height 600
        ystep (/ height max-y)
        step (/ width (count coll) )]
    [step (reduce (fn [prevpts [{s1 :status p1 :y} {s2 :status p2 :y} idx]]
                    (conj prevpts
                          [{:status s1 :y (- height (* ystep p1))}
                           {:status s2 :y (- height (* ystep p2))}
                           (* step idx)]))
                  [] coll)]))

;(defn handler [response]
;  (.log js/console (str response)))
;
;(defn error-handler [{:keys [status status-text]}]
;  (.log js/console (str "something bad happened: " status " " status-text)))

(defmacro time-exec
  "Evaluates expr and prints the time it took.  Returns the value of
 expr."
  {:added "1.0"}
  [expr]
  `(let [start# (. System (nanoTime))
         ret# ~expr]
     (/ (double (- (. System (nanoTime)) start#)) 1000000.0)) )

(defn time-remote-call [endpoint f]
  (go (let [start (js->clj (.getTime (js/Date.)))
            _ (swap! thread-count inc)
            response (<! (http/get endpoint {:with-credentials? false}))
            _ (swap! thread-count dec)
             t (double (- (js->clj (.getTime (js/Date.))) start))
            ]
        ;;enjoy your data
        (f                                                  ;t
          (:body response)
          (:status response)
          t

          ;(:body response)
          ))))

(defn make-remote-call [endpoint f]
  (go (let [response (<! (http/get endpoint {:with-credentials? false}))]
        ;;enjoy your data
        (f (:body response)))))







(defn transaction! []
  (time-remote-call "http://localhost:8080/compute" #(do
                                                       ;(println %)
                                                  ;(reset! test-field %2)
                                                  (reset! tx-time  {:status %2 :response %1 :y %3}))))

(defn add-point []

  (swap! points (fn [pts]
                  (let [new-pts (conj pts @tx-time)
                        ;c (count new-pts)
                        ]
                    (println "time add-point")
                    (into [] (take-last 64 new-pts))))))



(defonce time-updater2 (js/setInterval
                    #(do
                       (if (< @thread-count 1)
                         (do

                           (transaction!)
                           (reset! test-field @tx-time)
                           (add-point)
                           ))
                       ) 400))


(defn greeting [message]
  [:h1 message])

(defn clock []
  (let [time-str (-> @timer .toTimeString (clojure.string/split " ") first)]
    [:div.example-clock
     {:style {:color @time-color}}
     time-str]))


(defn color-input []
  [:div.color-input
   "Time color: "
   [:input {:type "text"
            :value @time-color
            :on-change #(reset! time-color (-> % .-target .-value))}]])

(defn test-component []
  [:div.color-input
   "test="
   @test-field
   [:p "req simult #=" @thread-count]
   ])

;;-------------------------------



(defonce slider
         (r/atom
           {:handle (g/point 500 50)
            :history []}))

(defn record-state [_ _ _ s]
  (swap! slider (fn [{:keys [history] :as coll}]
                  (assoc coll :history (conj history s)))))

(defn start-recording-history []
  (let [history (:history @slider)]
    (add-watch points :record record-state)))

(defn stop-recording-history []
  (remove-watch points :record))

(add-watch points :record record-state)

(defn get-bcr [svg-root]
  (-> svg-root
      r/dom-node
      .getBoundingClientRect))

(defn move-point [svg-root p]
  (fn [x y]
    (let [bcr (get-bcr svg-root)]
      (swap! points assoc p (g/point (- x (.-left bcr)) (- y (.-top bcr)))))))

(defn move-slider [svg-root p]
  (fn [x y]
    (let [new-x (-> (- x (.-left (get-bcr svg-root)))
                    (min 500)
                    (max 100))
          position (/ (- new-x 100)
                      (- 500 100))
          history (:history @slider)]
      (swap! slider assoc p (g/point new-x 50))
      (reset! points (nth history (int (* (dec (count history)) position)))))))

(defn plot [[step coll]]
  (mapcat (fn [[{s1 :status p1 :y} {s2 :status p2 :y} idx]] [(c/segment (g/point idx p1) (g/point (+ idx step) p2) idx)
                             ;(c/point {:on-drag (move-point svg-root :c)} (g/point idx p1))
                             (c/point {:point-colour (if (= s1 200) "blue" "red")} (g/point idx p1) idx)
                             ]
            ) coll))

(defn graph [pts]
  (->> (map (fn [p1 p2] [p1 p2]) pts (drop 1 pts))
       (reduce (fn [[acc prevpts] pt] (vector (inc acc) (conj prevpts (conj pt acc)))) [0 []] )
       second
       (rescale (apply max (map :y pts)))
       plot
       )

  )

(defn root [svg-root]
  (let [pts @points]
    [:g
     ;[c/triangle p1 p2 p3]
     ;[c/circle p c]
     ;[c/segment (get pts 0) (get pts 1)]
     ;[c/segment (get pts 1) (get pts 2)]
     ;(c/segment (get pts 2) (get pts 3))
     (graph pts)
     ;(map (fn [[p1 p2]] (c/segment p1 p2)) (map (fn [p1 p2] [p1 p2]) pts (drop 1 pts)))
     ;[c/segment (g/point 100 50) (g/point 500 50)]
     ;[c/rect {:on-drag (move-slider svg-root :handle)
     ;         :on-start stop-recording-history
     ;         :on-end start-recording-history} (:handle @slider)]
     ;[c/circle  (g/point 500 400) 4]
     ;(c/point {:on-drag nil} (g/point 100 400))
     ;[c/point {:on-drag (move-point svg-root :p1)} (g/point 150 (get pts 2))]
     ;[c/point {:on-drag (move-point svg-root :p2)} (get pts 3)]
     ;[c/point {:on-drag (move-point svg-root :p3)} (get pts 4)]
     ]))

(defn canvas [{:keys [width height]}]
  [:svg
   {:width (or width 800)
    :height (or height 600)
    :style {:border "1px solid black"}}
   [:text {:style {:-webkit-user-select "none"
                   :-moz-user-select "none"}
           :x 20 :y 20 :font-size 20}
    "The points are draggable and the slider controls history"]
   [root (r/current-component)]])


(defn simple-example [{:keys [width height] :as ctx}]
  [:div
   [greeting "Hello world2, it is now"]
   [clock]
   [color-input]
   [test-component]
   [canvas ctx]])

(defn ^:export run []
  (r/render [simple-example]
            (js/document.getElementById "app")))
