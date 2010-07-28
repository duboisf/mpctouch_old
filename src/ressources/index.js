Function.prototype.curry = function () {
    var slice = Array.prototype.slice,
        args = slice.apply(arguments),
        that = this;

    return function () {
        return that.apply( null, args.concat( slice.apply( arguments ) ) );
    }
}

Ext.ns( 'mpctouch' );
Ext.ns( 'mpctouch.rest' );

mpctouch.rest = {
    root: '/mpctouch/ressources',
    player: '/player',
    playlist: '/playlist'
};

mpctouch.main = function () {

    function playerRequest ( method, opts ) {

        opts.callback = opts.callback || function ( success, resp ) {
            if ( !success ) {
                Ext.Msg.alert( 'Command failure', 'Command ' + opts.command + ' failed' );
            }
        }
        
        method = method || 'PUT';

        opts.params = opts.params || {};

        Ext.Ajax.request({
                url: mpctouch.rest.root + mpctouch.rest.player + opts.command,
                method: method,
                success: opts.callback.curry( true ),
                failure: opts.callback.curry( false ),
                params: opts.params
        });
    }

    Ext.regModel('Song', {
        fields: [
            { name: 'title', type: 'string' },
            { name: 'artist',  type: 'string' },
            { name: 'album',  type: 'string' }
        ]
    });

    var songsStore = new Ext.data.JsonStore({
        model: 'Song',
        autoDestroy: true,
        sorters: 'title',
        getGroupString : function( record ) {
            return record.get('artist');
        },
        proxy: {
            type: 'ajax',
            url: mpctouch.rest.root + '/playlist/songs',
            reader: {
                type: 'json',
                root: 'songs',
                idProperty: 'title'
            }
        }
    });

    songsStore.load( {} );

    var playerGetRequest = playerRequest.curry( 'GET' );
    var playerPutRequest = playerRequest.curry( 'PUT' );
    var playerPostRequest = playerRequest.curry( 'POST' );
    var playerDeleteRequest = playerRequest.curry( 'DELETE' );

    var currentSongPanel = new Ext.Panel( {} );

    currentSongPanel.update( 'Test' );

    var playlistPanel = new Ext.Panel({
        layout: Ext.platform.isPhone ? 'fit' : {
            type: 'vbox',
            align: 'center',
            pack: 'center'
        },
        cls: 'demo-list',
        items: [{
            width: 300,
            height: 500,
            xtype: 'list',
            disclosure: {
                scope: songsStore,
                handler: function(record, btn, index) {
                    alert('Disclose more info for ' + record.get('title'));
                }
            },
            store: songsStore,
            tpl: '<tpl for="."><div class="song"><strong>{title}</strong><br>&nbsp;&nbsp;{album}</div></tpl>',
            itemSelector: 'div.song',
            singleSelect: true,
            grouped: true,
            indexBar: true
        }]
    });

    var carousel = new Ext.Carousel({
        flex: 1,
        animation: 'cube',
        items: [
            currentSongPanel,
            playlistPanel,
        {
          title: 'Tab 3',
          html: '3'
        }]
    });

    carousel.updateContent = function () {
        playerGetRequest({
            command: '/song/current',
            callback: function ( success, resp ) {
                if ( success ) {
                    var song = Ext.decode( resp.responseText );
                    var html = [];
                    for ( var prop in song ) {
                        if ( !( song[prop] instanceof Function ) ) {
                            html.push( prop + ': ' + song[prop] );
                        }
                    }
                    currentSongPanel.update( html.join( '<br>' ) );
                }
            }
        });
    };

    var slider = new Ext.form.Slider({});

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

    carousel.updateContent();

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
                handler: playerPutRequest.curry({
                    command: '/command/prev',
                    callback: carousel.updateContent
                })
            }, {
                text: 'stop',
                handler: playerPutRequest.curry( { command: '/command/stop' } )
            }, {
                text: 'play',
                handler: playerPutRequest.curry( { command: '/command/play' } )
            }, {
                text: 'next',
                handler: playerPutRequest.curry({
                    command: '/command/next',
                    callback: carousel.updateContent
                })
            }]
        }, slider
        ]
    });

    new Ext.Panel({
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
