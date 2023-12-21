# circuit-viewmodel-demo

Demo App for Circuit ViewModel bug https://github.com/slackhq/circuit/issues/1065

## The issue

If you run the app and navigation to "Detail" and back, you'll see `System.out` logs like:

```
DEMO: Factory.create(): 1703187143538 : null null
DEMO: init com.example.circuitviewmodeldemo.LoggingViewModel@43f4aa3 : 1703187143538
DEMO: Factory.create(): 1703187143538 : android.app.Application@eac8aae com.example.circuitviewmodeldemo.MainActivity@58280cb
DEMO: init com.example.circuitviewmodeldemo.LoggingViewModel@3d0e0a8 : 1703187143538
DEMO: onCleared(): com.example.circuitviewmodeldemo.LoggingViewModel@43f4aa3 : 1703187143538
DEMO: Factory.create(): 1703187145394 : null null
DEMO: init com.example.circuitviewmodeldemo.LoggingViewModel@eafcefc : 1703187145394
DEMO: Factory.create(): 1703187145394 : android.app.Application@eac8aae com.example.circuitviewmodeldemo.MainActivity@58280cb
DEMO: init com.example.circuitviewmodeldemo.LoggingViewModel@e674fc0 : 1703187145394
DEMO: onCleared(): com.example.circuitviewmodeldemo.LoggingViewModel@eafcefc : 1703187145394
```

Note how `Factory.create()` is invoked twice for the same id. 
First is the expected one, for the first composition for Circuit's per-screen-ViewModelStore.
Second is the bug, it's calling for the Activity's ViewModelStore.

The bug is also present if you remove the Factory and have a default constructor, 
though only one instance is leaked in that case.
