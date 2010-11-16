/**
 * Copyright (c) 2010, University of Manchester
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1) Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2) Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3) Neither the name of the University of Manchester nor the names of its
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 */

import flash.external.ExternalInterface;

/*import com.blitzagency.xray.util.XrayLoader;
import com.blitzagency.xray.logger.LogManager; */

import com.googlecode.vicovre.player.Controls;
import com.googlecode.vicovre.player.TimeSlider;
import com.googlecode.vicovre.player.ThumbnailSlider;
import com.googlecode.vicovre.player.VideoStream;

/**
 * The main player
 */
class com.googlecode.vicovre.player.Player extends MovieClip {

    // The log subsystem
    public var logger = null;

    // The NetConnection object
    private var netConnection:NetConnection = null;

    // The overall duration as reported by the metadata
    private var duration:Number = 0;

    // The vector of replay-layouts to be used
    private var layouts:Array = null;

    // The vector of annotation types used by the recording
    private var annTypes:Array = null;

    // The annotations of the recording
    private var annotations:Array = null;

    // The url of the session
    private var url:String = "";

    // the video-stream ids this vector contains all streams the
    // function of each stream is defined by the replay-layout
    private var videoStreams:Array = new Array();

    // The video streams accessed by name of layout position
    private var videoStreamsByName:Object = new Object();

    // The background of the player
    private var backgroundImage:MovieClip = null;

    // The image when buffering
    private var bufferingImage:MovieClip = null;

    // The buffering text field
    private var bufferText:TextField = null;

    // The controls of the player
    private var controls:Controls = null;

    // The slider of the time line
    private var timeSlider:TimeSlider = null;

    // The thumbnail slider
    private var thumbnailSlider:ThumbnailSlider = null;

    // True if the movie is playing
    private var playing:Boolean = false;

    // True if the movie is paused
    private var paused:Boolean = false;

    // True if the flash has loaded
    private var loaded:Boolean = false;

    // The click-to-play clip displayed over the video
    private var clickToPlay:MovieClip;

    // True if the video or screen is buffering
    private var buffering:Boolean = false;

    // The time at which playback was started
    private var startTime:Number = 0;

    // The width of the current layout
    private var layoutWidth = 0;

    // The height of the current layout
    private var layoutHeight = 0;

    // The current volume
    private var volume:Number = 50;

    public function Player() {
        trace("Starting");
        go();
    }

    private function go() {
        logger = new TraceLogger();

        this.layouts = new Array();

        Stage.align = "TL";
        Stage.scaleMode = "noScale";

        logger.debug("Stage:" + Stage.width + ", " + Stage.height);
        Stage.addListener(this);

        netConnection = new NetConnection();
        netConnection.connect(null);

        logger.debug("Attempting to read " + _root.uri);
        var initNetStream:NetStream = new NetStream(netConnection);
        initNetStream.onMetaData = this.onInitMetaDataCallback;
        initNetStream.onStatus =
            function(status:Object):Void {
                var logger = new TraceLogger();
                logger.debug(status["code"]);
                if (status["code"] == "NetStream.Play.StreamNotFound") {

                }
            };
        this.onInitMetaDataCallback.player = this;
        logger.debug("About to play");
        initNetStream.play(_root.uri);
    }

    private function xrayLoadComplete() {
        go();
    }

    private function xrayLoadError() {
        go();
    }

    /**
     * Called when the clip is loaded
     */
    public function onEnterFrame():Void {
        if (loaded || (Stage.width <= 0) || (Stage.height <= 0)) {
            return;
        }
        loaded = true;
        delete this.onEnterFrame;
    }

    public function play():Void {
        seek(0);
    }

    public function seek(time:Number):Void {
        clickToPlay._visible = false;
        logger.debug("Seek " + time );
        if (timeSlider !=null){
            timeSlider.start();
        }
        if (thumbnailSlider !=null){
            thumbnailSlider.start();
        }
        buffering = false;
        playing = true;
        paused = false;
        if (controls !=null) {
            controls.drawPause();
        }
        for (var i=0; i<videoStreams.length; i++){
            videoStreams[i].seek(time);
        }
    }

    public function pause():Void {
        if (playing && !paused) {
            paused = true;
        }
    }

    public function resume():Void {
        if (playing && paused) {
            paused = false;
        } else if (!playing) {
            play();
        }
    }

    public function isPlaying():Boolean {
        return playing && !paused;
    }

    public function getTime():Number {
        var time=0;
        for (var i=0; i<videoStreams.length; i++){
            time=Math.max(time,videoStreams[i].getTime());
        }
        return time;
    }

    public function getDuration():Number {
        return this.duration;
    }

    public function setVolume(volume:Number):Void {
        this.volume = volume;
        for (var i = 0; i < videoStreams.length; i++) {
            videoStreams[i].setVolume(volume);
        }
    }

    public function getVolume():Number {
        return volume;
    }

    public function onInitMetaData(data:Object):Void {
        this.url = data["url"];
        this.startTime = data["startTime"] / 1000;
        this.duration = data["duration"] / 1000;
        this.annTypes = data["annotationTypes"];
        this.annotations = data["annotations"];
        this.videoStreams = new Array();
        var ctpdims:Object={area:0,x:0,y:0,width:0,height:0};

        _x = 0;
        _y = 0;

        backgroundImage = createEmptyMovieClip("background", 0);

        netConnection = new NetConnection();
        netConnection.connect(null);

        // get first layout (deal with changes of layouts later)
        var layout = data["layouts"][0];
        var videoDepth = 100;
        var layoutpos = layout["layoutPositions"];
        var minX = 0;
        var minY = 0;
        for (var l = 0; l < layoutpos.length; l++) {
            var lp = layoutpos[l];
            if (lp["type"] == "Annotation"){
                thumbnailSlider = new ThumbnailSlider(this,
                    1000, data["thumbnails"], data["annotations"],
                    0x9a9393, 0x555555, 0x4b4a4a, 0x563E3E, 0xFFFFFF);
            }
            if (lp["type"] == "Slider"){
                timeSlider = new TimeSlider(this, 2000, 10,
                    0x9a9393, 0x555555, 0x4b4a4a, 0x000000, 0x746D6D,
                    annotations, annTypes);
                var h = annTypes.length;
                if (h == 0) {
                    h = 1;
                }
                lp["height"] = h * 25;
            }
            if (lp["type"] == "Controls"){
                controls = new Controls(this, 30,
                    0x9a9393, 0x555555, 0xFFFFFF, 0x4b4a4a);
            }
            if (lp["type"] == "video"){
                var video = new VideoStream(this, lp["name"], videoDepth,
                    lp["stream"], netConnection, lp["audioStreams"]);
                videoDepth++;
                var name = lp["name"];
                this.videoStreamsByName[name] = this.videoStreams.length;
                this.videoStreams.push(video);
            }

            if ((Number(lp["x"]) + Number(lp["width"])) > layoutWidth) {
                layoutWidth = Number(lp["x"]) + Number(lp["width"]);
            }
            if ((Number(lp["y"]) + Number(lp["height"])) > layoutHeight) {
                layoutHeight = Number(lp["y"]) + Number(lp["height"]);
            }
            if ((minX == 0) || (Number(lp["x"]) < minX)) {
                minX = Number(lp["x"]);
            }
            if ((minY == 0) || (Number(lp["y"]) < minY)) {
                minY = Number(lp["y"]);
            }
        }
        layoutWidth += minX;
        layoutHeight += minY;
        for (var i = 0; i < videoStreams.length; i++) {
            videoStreams[i].setUrl(this.url, this.videoStreams);
        }

        ExternalInterface.addCallback("seek", this, this.seek);
        ExternalInterface.addCallback("pause", this, this.pause);
        ExternalInterface.addCallback("resume", this, this.resume);
        ExternalInterface.addCallback("stop", this, this.stop);

        bufferingImage = createEmptyMovieClip("bufferingImage", 65535);
        bufferingImage._visible = false;
        bufferingImage._alpha = 10;

        var bufferTextFormat:TextFormat = new TextFormat();
        bufferTextFormat.align = "center";
        bufferTextFormat.color = 0xFFFFFF;
        bufferTextFormat.size = 20;
        bufferTextFormat.font = "_serif";

        bufferText = bufferingImage.createTextField("text", 0, 0, 0, 300, 30);
        bufferText.setNewTextFormat(bufferTextFormat);
        bufferText.text = "Buffering 0%...";

        this.onPlayPress.player = this;
        setVolume(50);

        clickToPlay = createEmptyMovieClip("ClickToPlay", 500);
        clickToPlay._x = 0;
        clickToPlay._y = 0;
        clickToPlay.onPress = this.onPlayPress;

        this.layouts = data["layouts"];

        doResize();

        setInterval(this, "update", 250);
    }

    public function onResize() {
        doResize();
        if (playing && paused) {
            seek(getTime());
            paused = true;
        } else if (playing && !paused) {
            seek(getTime());
        }
    }

    public function doResize() {
        var scaleX = Stage.width / layoutWidth;
        var scaleY = Stage.height / layoutHeight;
        var scale = Math.min(scaleX, scaleY);

        backgroundImage.beginFill(0xcecccc);
        backgroundImage.moveTo(0, 0);
        backgroundImage.lineTo(Stage.width + 15, 0);
        backgroundImage.lineTo(Stage.width + 15, Stage.height + 15);
        backgroundImage.lineTo(0, Stage.height + 15);
        backgroundImage.endFill();

        if (layouts != null) {
            var layout = layouts[0];
            var layoutpos = layout["layoutPositions"];
            for (var l = 0; l < layoutpos.length; l++) {
                var lp = layoutpos[l];
                if (lp["type"] == "Annotation") {
                    thumbnailSlider.setDims(Number(lp["x"]) * scale,
                                            Number(lp["y"]) * scale,
                                            Number(lp["width"]) * scale,
                                            Number(lp["height"]) * scale);
                }
                if (lp["type"] == "Slider") {
                    var h = annTypes.length;
                    if (h == 0) {
                        h = 1;
                    }
                    timeSlider.setDims(Number(lp["x"]) * scale,
                                       Number(lp["y"]) * scale,
                                       Number(lp["width"]) * scale,
                                       Number(h * 25) * scale);
                    timeSlider.setSliderTime(startTime);
                }
                if (lp["type"] == "Controls") {
                    controls.setDims(Number(lp["x"]) * scale,
                                     Number(lp["y"]) * scale,
                                     Number(lp["width"]) * scale,
                                     Number(lp["height"]) * scale);
                }
                if (lp["type"] == "video") {
                    var name = lp["name"];
                    var index = this.videoStreamsByName[name];
                    var video = this.videoStreams[index];
                    video.setDims(Number(lp["x"]) * scale,
                                  Number(lp["y"]) * scale,
                                  Number(lp["width"]) * scale,
                                  Number(lp["height"]) * scale);
                }

            }
        }

        bufferingImage.clear();
        bufferingImage.beginFill(0x000080);
        bufferingImage.moveTo(0, 0);
        bufferingImage.lineTo(Stage.width, 0);
        bufferingImage.lineTo(Stage.width, Stage.height);
        bufferingImage.lineTo(0, Stage.height);
        bufferingImage.endFill();

        bufferingImage.text._x = ((layoutWidth * scale) / 2)
            - (bufferingImage.text._width / 2);
        bufferingImage.text._y = ((layoutHeight * scale) / 2)
            - (bufferingImage.text._height / 2);

        clickToPlay.clear();
        for (var i = 0; i < videoStreams.length; i++) {
            var dims = videoStreams[i].getDims();
            clickToPlay.beginFill(0x000000);
            clickToPlay.moveTo(dims.x, dims.y);
            clickToPlay.lineTo(dims.x + dims.width, dims.y);
            clickToPlay.lineTo(dims.x + dims.width, dims.y + dims.height);
            clickToPlay.lineTo(dims.x, dims.y + dims.height);
            clickToPlay.endFill();

            clickToPlay.beginFill(0xFFFFFF);
            var centerX = dims.x + (dims.width / 2);
            var centerY = dims.y + (dims.height / 2);
            clickToPlay.moveTo(centerX - 25, centerY - 25);
            clickToPlay.lineTo(centerX - 25, centerY + 25);
            clickToPlay.lineTo(centerX + 25, centerY);
            clickToPlay.lineTo(centerX - 25, centerY - 25);
            clickToPlay.endFill();
        }
    }

    public function onInitMetaDataCallback(data:Object):Void {
        var player:Player = arguments.callee.player;
        player.onInitMetaData(data);
    }

    public function onPlayPress():Void {
        var player:Player = arguments.callee.player;
        player.seek(player.startTime);
    }

    public function update():Void {
        if (paused) {
            for (var i=0; i<videoStreams.length; i++){
                videoStreams[i].pause(true);
            }
            buffering = true;
        } else if (playing) {
            var isFinished:Boolean = true;
            for (var i=0; i<videoStreams.length; i++){
                if (!videoStreams[i].isFinished()) {
                    isFinished = false;
                    break;
                }
            }
            if (isFinished) {
                clickToPlay._visible = true;
                timeSlider.stop();
                thumbnailSlider.stop();
                timeSlider.setTimeText(duration);
            } else if (buffering) {
                var isBufferFull:Boolean = true;
                for (var i=0; i<videoStreams.length; i++){
                    if (!videoStreams[i].isBufferFull()) {
                        //logger.debug("stream["+i+"] Buffer not full");
                        isBufferFull = false;
                        break;
                    }
                }
                if (isBufferFull) {
                    for (var i=0; i<videoStreams.length; i++){
                        videoStreams[i].pause(false);
                        videoStreams[i].stopBuffering();
                        logger.debug("stream["+i+"] started");
                    }
                    buffering = false;
                    bufferingImage._visible = false;
                } else {
                    var timeSum:Number=0;
                    var lengthSum:Number=0;
                    for (var i=0; i<videoStreams.length; i++){
                        if (videoStreams[i].getBufferLength()>=0.5) {
                            logger.debug("stream["+i+"] paused");
                            videoStreams[i].pause(true);
                            videoStreams[i].startBuffering();
                        }
                        timeSum+=videoStreams[i].getBufferTime();
                        lengthSum+=videoStreams[i].getBufferLength();
                    }
                    var bufferingPercent:Number = lengthSum/timeSum;
                    bufferingPercent *= 100;
                    bufferText.text = "Buffering "
                        + Math.round(bufferingPercent) + "%...";
                    bufferingImage._visible = true;
                }
            } else {
                var isBufferEmpty:Boolean = false;
                for (var i=0; i<videoStreams.length; i++){
                    if (videoStreams[i].isBufferEmpty()) {
                        //logger.debug("stream["+i+"] Buffer empty: " + videoStreams[i].getTime() + " of " + videoStreams[i].getDuration());
                        isBufferEmpty = true;
                        break;
                    }
                }
                if (isBufferEmpty) {
                    logger.debug("start buffering");
                    buffering = true;
                }
            }
        }
    }

    public function stop() {
        for (var i=0; i<videoStreams.length; i++){
            videoStreams[i].stop();
        }
        netConnection.close();
    }
}
