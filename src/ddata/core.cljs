(ns ddata.core
  (:require [clojure.string :refer [split]]
            [reagent.interop :refer-macros [$ $!]]))


(def skeleton-img (atom nil))
(def skeleton-map (atom nil))
(def skeleton-frame (atom 0))
(def swguy-img (atom nil))
(def swguy-map (atom nil))
(def swguy-frame (atom 0))

(def game-loop (atom nil))


(defn get-canvas []
  ($ js/document querySelector "#app"))


(defn load-image [container url]
  (let [img (js/Image.)]
    ($! img :onload #(reset! container img))
    ($! img :src url)))


(defn load-asset [container url]
  (-> (js/fetch url)
      ($ then #(.json %))
      ($ then #(reset! container (js->clj % :keywordize-keys true)))))


(defn draw-image [ctx img & {:keys [sx sy sw sh dx dy dw dh]}]
  ($ ctx drawImage img sx sy sw sh dx dy dw dh))


(defn render-image [ctx img img-map curr-frame y-offset]
  (let [image-name (get-in img-map [:meta :image])
        [frame-prefix frame-suffix] (split image-name #"\.")
        frame-name (keyword (str frame-prefix curr-frame "." frame-suffix))
        frame      (get-in img-map [:frames frame-name :frame])]
    (draw-image ctx img
      :sx (:x frame)
      :sy (:y frame)
      :sw (:w frame)
      :sh (:h frame)
      :dx 100
      :dy y-offset
      :dw (:w frame)
      :dh (:h frame))))


(defn render [ctx]
  (when (and @skeleton-img @swguy-img @skeleton-map @swguy-map)
    ($ ctx clearRect 0 0 (.-innerWidth js/window) (.-innerHeight js/window))
    ($! ctx :fillStyle "grey")
    ($ ctx fillRect 0 0 (.-innerWidth js/window) (.-innerHeight js/window))

    (render-image ctx @skeleton-img @skeleton-map @skeleton-frame 50)
    (render-image ctx @swguy-img @swguy-map @swguy-frame 150)

    (swap! skeleton-frame #(mod (inc %) 13))
    (swap! swguy-frame #(mod (inc %) 4))))


(defn start []
  (let [canvas     (get-canvas)
        ctx        ($ canvas getContext "2d")
        frame-rate (/ 1000 20)]
    ($! canvas :width ($ js/window :innerWidth))
    ($! canvas :height ($ js/window :innerHeight))

    (load-image skeleton-img "/img/skeleton-walk.png")
    (load-image swguy-img "/img/swguy_run.png")

    (load-asset skeleton-map "/assets/skeleton-walk.json")
    (load-asset swguy-map "/assets/swguy_run.json")

    (reset!
      game-loop
      (js/setInterval (partial render ctx) frame-rate))))


(defn stop []
  (when-let [loop @game-loop]
    (js/clearInterval loop)))


(defn ^:export init []
  (start))

