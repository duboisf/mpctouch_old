//Object.prototype.method = function ( name, func ) {
//    this[name] = func;
//}

Function.prototype.curry = function () {
    var slice = Array.prototype.slice,
        args = slice.apply(arguments),
        that = this;

    return function () {
        return that.apply( null, args.concat( slice.apply( arguments ) ) );
    }
}

Ext.setup({
    tabletStartupScreen: 'tablet_startup.png',
    phoneStartupScreen: 'phone_startup.png',
    icon: 'icon.png',
    glossOnIcon: false,
    onReady: function () {

        function playerRequest ( opts ) {

            opts.callback = opts.callback || function ( success, resp ) {
                if ( !success ) {
                    Ext.Msg.alert( 'Command failure', 'Command ' + opts.command + ' failed' );
                }
            }
            
            opts.method = opts.method || 'PUT';

            opts.params = opts.params || {};

            return function () {
                Ext.Ajax.request({
                    url: '/mpctouch/ressource/player/' + opts.command,
                    method: opts.method,
                    success: opts.callback.curry( true ),
                    failure: opts.callback.curry( false ),
                    params: opts.params
                });
            }
        }

        var carousel1 = new Ext.Carousel({
            flex: 1,
            animation: 'cube',
            items: [{
              html: '<h1>Carousel</h1><p>Navigate the two carousels on this page by swiping left/right or clicking on one side of the circle indicators below.</p>'
            }, {
              title: 'Tab 2',
              html: '2'
            }, {
              title: 'Tab 3',
              html: '3'
            }]
        });

        var slider = new Ext.form.Slider({});

        // Fetch initial volume value to setup slide, then add change listener
        playerRequest({
            command: 'volume',
            method: 'GET',
            callback: function ( success, resp ) {
                if ( success ) {
                    var json = Ext.decode( resp.responseText );
                    slider.setValue( json.volume );
                    slider.on( 'change', function ( slider, thumb, oldVal, newVal ) {
                        playerRequest({
                            command: 'volume/' + newVal,
                            method: 'PUT'
                        })();
                    });
                }
            }
        })();

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
                    handler: playerRequest( { command: 'prev' } )
                }, {
                    text: 'stop',
                    handler: playerRequest( { command: 'stop' } )
                }, {
                    text: 'play',
                    handler: playerRequest( { command: 'play' } )
                }, {
                    text: 'next',
                    handler: playerRequest( { command: 'next' } )
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
            items: [ carousel1, controls ]
        });
    }
})
