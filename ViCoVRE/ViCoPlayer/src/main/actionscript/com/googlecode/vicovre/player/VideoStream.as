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

import flash.geom.Rectangle;

import com.googlecode.vicovre.player.Player;
import com.googlecode.vicovre.player.Utils;

class com.googlecode.vicovre.player.VideoStream {

    private var parent:Player = null;
    private var videoName:String="";
    private var videoNetStream:NetStream = null;
    private var videoDuration:Number = -1;
    private var videoUrl:String = "";
    private var audioStreams:Array = null;
    private var videoDisplay:MovieClip = null;
    private var videoStream:Number = 0;
    private var x:Number = 0;
    private var y:Number = 0;
    private var width:Number = 0;
    private var height:Number = 0;
    private var depth:Number = 0;
    private var netConnection:NetConnection;
    private var audio:MovieClip = null;
    private var sound:Sound = null;
    private var bufferFlushing:Boolean = false;
    private var bufferTime:Number = 5;
    private var smallBufferTime:Number = 5;
    private var largeBufferTime:Number = 180;
    private var zoom:Number = 100;
    private var videoX:Number = 0;
    private var videoY:Number = 0;

    public function VideoStream(parent:Player, name:String, depth:Number,
                stream:Array, netConnection:NetConnection, audioStreams:Array) {
        this.parent = parent;
        this.videoName = name;

        this.depth = depth;
        this.videoDuration = stream["duration"];
        this.videoStream = stream["ssrc"];
        this.videoDisplay = parent.attachMovie("VideoDisplay", "dummyDisplay",
            depth);
        this.netConnection = netConnection;
        this.audioStreams = audioStreams;
        this.videoNetStream = new NetStream(netConnection);
        var statusFunction = function(status:Object):Void {
            var strm:VideoStream = arguments.callee.stream;
            strm.updateStatus(status);
        };
        statusFunction.stream = this;
        this.videoNetStream.onStatus = statusFunction;
        this.videoDisplay.video.attachVideo(videoNetStream);
        if (audioStreams != null && audioStreams.length > 0) {
            this.audio = this.parent.createEmptyMovieClip("audio" + name,
                this.parent.getNextHighestDepth());
            this.audio.attachAudio(videoNetStream);
            this.sound = new Sound(audio);
            this.sound.setVolume(0);
        }

    }

    public function setDims(x:Number, y:Number, width:Number, height:Number) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.videoDisplay.video._width = this.width;
        this.videoDisplay.video._height = this.height;
        this.videoDisplay._x = this.x;
        this.videoDisplay._y = this.y;
    }

    public function setUrl(url:String, syncStreams:Array){
        this.videoUrl = url;
        this.parent.logger.debug("URL = " + url);
        this.parent.logger.debug("indexof ? = " + url.indexOf("?"));
        if (url.indexOf("?") != -1) {
            this.videoUrl += "&";
        } else {
            this.videoUrl += "?";
        }
        this.videoUrl += "duration=" + this.videoDuration
            + "&video=" + this.videoStream;
        for (var i = 0; i < syncStreams.length; i++) {
            var ss = syncStreams[i].getVideoStream();
            if (ss != this.videoStream){
                this.videoUrl += "&sync=" + ss;
            }
            if ((audioStreams == null) || (audioStreams.length == 0)) {
                var audio:Array = syncStreams[i].getAudioStreams();
                if (audio != null) {
                    for (var j = 0; j < audio.length; j++) {
                        this.videoUrl += "&sync=" + audio[j]["ssrc"];
                    }
                }
            }
        }
        for (var i = 0; i < audioStreams.length; i++) {
            var as=audioStreams[i];
            this.videoUrl += "&audio=" + as["ssrc"];
        }
    }

    public function setVolume(volume:Number) {
        if (sound != null) {
            sound.setVolume(volume);
        }
    }

    public function getDims():Object{
        var dims:Object={x:x, y:y, width:width, height:height};
        return dims;
    }

    public function updateStatus(status:Object) {
        if (status["code"] == "NetStream.Play.StreamNotFound") {
            videoDisplay.beginFill(0x000000);
            videoDisplay.moveTo(0, 0);
            videoDisplay.lineTo(0, videoDisplay._height);
            videoDisplay.lineTo(videoDisplay._width, videoDisplay._height);
            videoDisplay.lineTo(videoDisplay._width, 0);
            videoDisplay.endFill();
            var error:TextField = videoDisplay.createTextField("error",
                10, 0, (videoDisplay._height / 2) - 10,
                videoDisplay._width, 20);
            var format:TextFormat = new TextFormat();
            format.align = "center";
            format.size = 20;
            error.setTextFormat(format);
            error.text = "Error loading stream";
            this.videoDuration = 0;
            parent.logger.debug("Error loading video");
            this.bufferFlushing = true;
        } else if (status["code"] == "NetStream.Buffer.Flush") {
            this.bufferFlushing = true;
        }
    }

    public function pause(state:Boolean){
        this.videoNetStream.pause(state);
    }

    public function getTime():Number {
        return videoNetStream.time;
    }

    public function getDuration():Number {
        return videoDuration;
    }

    public function isFinished():Boolean {
        return bufferFlushing && (videoNetStream.bufferLength == 0);
    }

    public function isBufferFull():Boolean {
        return (isFinished() || bufferFlushing || (videoNetStream.bufferLength > this.bufferTime));
    }

    public function isBufferEmpty():Boolean {
        return (!isFinished() && !bufferFlushing && (videoNetStream.bufferLength <= 1));
    }

    public function startBuffering():Void {
        this.videoNetStream.setBufferTime(this.bufferTime);
    }

    public function finishBuffering():Void {
        this.videoNetStream.setBufferTime(this.largeBufferTime);
    }

    public function seek(time:Number):Void {
        var seekUrl = this.videoUrl + "&width=" + Math.round(this.width)
                                    + "&height=" + Math.round(this.height);
        this.videoNetStream.close();
        this.videoNetStream.setBufferTime(0);
        bufferFlushing = false;
        if (time < this.videoDuration) {
            if ((this.videoDuration - time) < this.smallBufferTime) {
                this.bufferTime = (this.videoDuration - time) / 2;
            } else {
                this.bufferTime = this.smallBufferTime;
            }
            this.videoNetStream.setBufferTime(this.bufferTime);
            this.videoNetStream.play(seekUrl + "&start=" + time);
        } else {
            this.bufferTime = 0.5;
            this.videoNetStream.setBufferTime(this.bufferTime);
            this.videoNetStream.play(seekUrl + "&start="
                + (this.videoDuration - 1));
        }
    }

    public function stop() {
        this.videoNetStream.close();
    }

    public function getBufferLength():Number {
        return videoNetStream.bufferLength;
    }

    public function getBufferTime():Number {
        return videoNetStream.bufferTime;
    }

    public function getBytesLoaded():Number {
        return videoNetStream.bytesLoaded;
    }

    public function getBytesTotal():Number {
        return videoNetStream.bytesTotal;
    }

    public function getVideoStream():Number {
        return videoStream;
    }

    public function getAudioStreams():Array {
        return audioStreams;
    }
}
