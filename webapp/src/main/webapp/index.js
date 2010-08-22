"use strict";
/*global Ext, mpctouch */

Function.prototype.method = function ( name, func ) {
    if ( !this.prototype[name] ) {
        this.prototype[name] = func;
    }
    return this;
};

Function.method( 'curry', function () {
    var slice = Array.prototype.slice,
        args = slice.apply(arguments),
        that = this;

    return function () {
        return that.apply( null, args.concat( slice.apply( arguments ) ) );
    };
});

Array.method( 'each', function (func, scope) {
    for (var i = 0; i < this.length; i++ ) {
        func.call( scope || this[i], this[i], i, this );
    }
});

Array.method( 'map', function (func, scope) {
    var results = [];
    this.each( function (e, i, arr) {
        results.push( func.call( scope || e, e, i, arr ) );
    });
    return results;
});

String.method( 'startsWith', function (str) {
    var match = this.match( '^' + str );
    if ( match ) {
        return match[0] === str;
    }
    return false;
});

Ext.ns( 'mpctouch' );
Ext.ns( 'mpctouch.rest' );

mpctouch.rest = {
    root: '/mpctouch/atmosphere'
};

mpctouch.events = {
    player: {
        next: 'mpctouch.events.player.next',
        prev: 'mpctouch.events.player.prev',
        stop: 'mpctouch.events.player.stop',
        play: 'mpctouch.events.player.play'
    },
    playlist: {
        songs: 'mpctouch.events.playlist.songs'
    }
};

mpctouch.main = function () {

    Ext.Ajax.timeout = 900000;

    function mpcRequest ( urlprefix, method, opts ) {
        opts.callback = opts.callback || function ( success, resp ) {
            if ( !success ) {
                alert( 'Command ' + opts.command + ' failed' );
            }
        };
        
        method = method || 'PUT';
        opts.params = opts.params || {};
        opts.command = opts.command || '';

        Ext.Ajax.request({
                url: mpctouch.rest.root + urlprefix + opts.command,
                method: method,
                success: opts.callback.curry( true ),
                failure: opts.callback.curry( false ),
                params: opts.params
        });
    }

    var playerRequest = mpcRequest.curry( '/player' );
    var playerGetRequest = playerRequest.curry( 'GET' );
    var playerPutRequest = playerRequest.curry( 'PUT' );
    var playerPostRequest = playerRequest.curry( 'POST' );
    var playerDeleteRequest = playerRequest.curry( 'DELETE' );

    var playlistRequest = mpcRequest.curry( '/player/playlist' );
    var playlistGetRequest = playlistRequest.curry( 'GET' );
    var playlistPutRequest = playlistRequest.curry( 'PUT' );
    var playlistPostRequest = playlistRequest.curry( 'POST' );
    var playlistDeleteRequest = playlistRequest.curry( 'DELETE' );

    var cometSuspend = mpcRequest.curry( '/player', 'GET' );

    Ext.regModel( 'Song', {
        fields: [
            { name: 'position', type: 'int' },
            { name: 'title', type: 'string' },
            { name: 'artist',  type: 'string' },
            { name: 'album',  type: 'string' }
        ]
    });

    var songsStore = new Ext.data.JsonStore({
        model: 'Song',
        autoDestroy: true,
        getGroupString : function( rec ) {
            return rec.get( 'artist' ) + ' - ' + rec.get( 'album' );
        },
        proxy: {
            type: 'ajax',
            url: mpctouch.rest.root + '/player/playlist/songs',
            reader: {
                type: 'json',
                root: 'songs',
                idProperty: 'position'
            }
        }
    });

    songsStore.load( {} );

    var currentSongPanel = new Ext.Panel( {} );

    var playlistPanel = new Ext.Panel({
        layout: Ext.platform.isPhone ? 'fit' : {
            type: 'vbox',
            align: 'center',
            pack: 'center'
        },
        items: [{
            width: 300,
            height: 500,
            xtype: 'list',
            disclosure: {
                scope: songsStore,
                handler: function( record, btn, index ) {
                    playlistPutRequest({
                        command: '/play',
                        params: { index: index }
                    });
                }
            },
            store: songsStore,
            tpl: '<tpl for="."><div class="song">{title}</div></tpl>',
            itemSelector: 'div.song',
            singleSelect: true,
            grouped: false
        }]
    });

    var carousel = new Ext.Carousel({
        flex: 1,
        items: [
            currentSongPanel,
            playlistPanel
        ]
    });

    var slider = new Ext.form.Slider({});

    function eventDispatcher () {

        function updateCurrentSongPanel ( song ) {
            var html = [];
            for ( var prop in song ) {
                if ( !( song[prop] instanceof Function ) && !prop.startsWith( '@' ) ) {
                    html.push( prop + ': ' + song[prop] );
                }
            }
            currentSongPanel.update( html.join( '<br>' ) );
        }

        return {
            prev: function ( song ) {
                updateCurrentSongPanel( song );
            },
            play: function ( song ) {
                updateCurrentSongPanel( song );
            },
            next: function ( song ) {
                updateCurrentSongPanel( song );
            },
            stop: function () {
                currentSongPanel.update( 'Stopped' );
            }
        };
    }

    var dispatcher = eventDispatcher();

    function cometLoop() {
        cometSuspend({
            callback: function ( success, resp ) {
                if ( success ) {
                    var respText = Ext.decode( resp.responseText );
                    if ( dispatcher[respText.event] instanceof Function ) {
                        dispatcher[respText.event]( respText.data );
                    }
                    cometLoop();
                } else {
                    cometLoop.defer( 5000 );
                }
            }
        });
    }

    cometLoop();

    // Fetch initial volume value to setup slide, then add change listener
    playerGetRequest({
        command: '/volume',
        callback: function ( success, resp ) {
            if ( success ) {
                var json = Ext.decode( resp.responseText );
                slider.setValue( json.volume );
                slider.on( 'change', function ( slider, thumb, oldVal, newVal ) {
                    playerPutRequest( { command: '/volume/' + newVal } );
                });
            }
        }
    });

    var controls = new Ext.Panel({
        layout: {
            type: 'vbox',
            align: 'stretch'
        },
        items: [{
            layout: {
                type: 'hbox',
                align: 'stretch'
            },
            defaults: { 
                xtype: 'button',
                flex: 1
            },
            items: [{
                text: 'prev',
                handler: playerPutRequest.curry({ command: '/command/prev' })
            }, {
                text: 'stop',
                handler: playerPutRequest.curry( { command: '/command/stop' } )
            }, {
                text: 'play',
                handler: playerPutRequest.curry( { command: '/command/play' } )
            }, {
                text: 'next',
                handler: playerPutRequest.curry({ command: '/command/next' })
            }]
        }, slider
        ]
    });

    var mainPanel = new Ext.Panel({
        fullscreen: true,
        layout: {
            type: 'vbox',
            align: 'stretch'
        },
        items: [ carousel, controls ]
    });
};

Ext.setup({
    tabletStartupScreen: 'tablet_startup.png',
    phoneStartupScreen: 'phone_startup.png',
    icon: 'icon.png',
    glossOnIcon: false,
    onReady: mpctouch.main
});
