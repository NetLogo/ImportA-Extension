# Import-A

## What is it?

In short: A NetLogo extension for running `import-*` primitives from plain text and base64 strings, with NetLogo Web compatibility.  Now for the longer explanation....

`import-a` is an extension that supplies some primitives that complement `import-drawing`, `import-pcolors`, `import-pcolors-rgb`, and `import-world`.  Functionally, there is little difference between, say, `import-world` and `import-a:world`.  However, the primitives in this extension aim to be more flexible than the standard `import-world`.

Recall that `import-world` will only import a world from a file on your computer; you give it a string as an argument, and that string is treated as a file path.  This is the only way to call `import-world`, and if your world is available at a URL, or you wanted to build the world file text from NetLogo code (for some ungodly reason), you will first need to convert those sources to files, and *then* run `import-world`.

With `import-a`, though, the `import-a:world` primitive accepts a string *containing the plain text contents of the file* as its argument, and `import-a:drawing`, `import-a:pcolors`, and `import-a:pcolors-rgb` accept base64-encoded plain text strings as their respective arguments.  This means that your source for the world/drawing doesn't matter; as long as it was converted into a meaningful string, `import-a` is perfectly happy to import it.

`import-a` works particularly well in conjunction with the `fetch` extension, which converts file paths and URLs to their string contents.

## What problem does this solve?

While it has some benefits for importing content from URLs, realistically, its goal is to facilitate `import-world` (or the like) in NetLogo Web.  That is to say that a version of this extension exists for NetLogo Web with the same exact API, so world-/drawing-importing models can be written to run in desktop NetLogo and also work exactly the same in NetLogo Web (and vice versa).

The source of this problem is the single-threaded execution model in JavaScript (JavaScript being what NetLogo Web does and must use as its underlying programming language).  Since single-threaded execution leads to poor user experience when doing long-running tasks like reading files or making web requests, JavaScript instead defers the execution of those long-running tasks by having them run "asynchronously".  But we can't run `import-drawing` (or the like) synchronously in desktop NetLogo and asynchronously in NetLogo Web, and still guarantee the same model behavior.  That's where `fetch` and `import-a` come in.

With `fetch`, we perform `fetch:url-async url callback` to retrieve a URL asynchronously from `url` and then call `callback` once it has completed running.  But, if we want to import a drawing (or the like) asynchronously like this, the standard `import-drawing` only accepts a file path and tries to read it synchronously, so we need to provide a different `import-drawing` (i.e. `import-a:drawing`) that will take an asynchronously-read and import the drawing from a string version of the contents of the file.

## Primitives

| Prim Name     | Arguments             | Behavior
| ------------- | --------------------- | --------
| `drawing`     | *base64*              | [See `import-drawing`](https://ccl.northwestern.edu/netlogo/docs/dictionary.html#import-drawing)
| `pcolors`     | *base64*              | [See `import-pcolors`](https://ccl.northwestern.edu/netlogo/docs/dictionary.html#import-pcolors)
| `pcolors-rgb` | *base64*              | [See `import-pcolors-rgb`](https://ccl.northwestern.edu/netlogo/docs/dictionary.html#import-pcolors-rgb)
| `world`       | *text*                | [See `import-world`](https://ccl.northwestern.edu/netlogo/docs/dictionary.html#import-world)

## Example Code

This extension was primarily intended as a companion to the `fetch` extension, so our example code will use that.

```netlogo
extensions [import-a fetch]

; Basic printing of a string (no extensions involved)
to test-fetch-reporter
  clear-all
  show "I'm a little reporter, short and stout.  Here is my input.  Here is my out."
end

; Printing of the contents of a file, using the synchronous primitive in this extension
to test-fetch-file-sync
  clear-all
  show (fetch:file user-file)
end

; Printing of the contents of a file, using the asynchronous primitive in this extension
to test-fetch-file-async
  clear-all
  fetch:file-async user-file show
end

; Printing of the contents of a file, async, without using the 'concise' anonproc syntax
to test-fetch-file-verbose-syntax
  clear-all
  fetch:file-async user-file [text -> show text]
end

; Printing of the content from a URL, using the synchronous primitive in this extension
to test-fetch-url-sync
  clear-all
  show (fetch:url (word "file://" user-file))
end

; Printing of the content from a URL
to test-fetch-url-async
  clear-all
  fetch:url-async (word "file://" user-file) show
end

; Importing world state from a file
to test-world-file
  clear-all
  fetch:file-async user-file import-a:world
end

; Importing world state from a file and then running some other code once it has completed
to test-world-file-and-then
  clear-all
  fetch:file-async user-file [
    text ->
      import-a:world text
      show "Success!"
  ]
end

; Importing world state from a URL
to test-world-url
  clear-all
  fetch:url-async (word "file://" user-file) import-a:world
end

; Importing world state from a URL and then running some other code once it has completed
to test-world-url-and-then
  clear-all
  fetch:url-async user-file [
    text ->
      import-a:world text
      show "Success!"
  ]
end
```

## Drawing Layer Example

A common use of `import-a:drawing` is to setup a background for use during a model run.  On NetLogo desktop this is a simple task using `import-drawing` with a local file.  On NetLogo Web, though, we only have access to the `fetch:url-async` primitive to get our image data.  As such we have a few extra considerations when making models:

- Because the image will be loaded asynchronously from the rest of the model setup, we need a way to wait to actually start the model until the image is displayed.  Fortunately the use of forever buttons, `clear-all`, and the `reset-ticks` primitives make this pretty straightforward.
- We have to make sure [the Cross-Origin Resource Sharing (CORS) rules](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS) of the web server hosting the image allow us to load the image from netlogoweb.org (or wherever the model is running from).  Setting up CORS is different for every web server, but it's a common task so it's usually well-documented.

```netlogo
extensions [ fetch import-a ]

to setup
  ; This clears the ticks, so a **go** button will be disabled until they are reset
  clear-all

  ; The CCL website server is setup to allow https://netlogoweb.org to fetch resources from it (CORS).
  let image-url "https://ccl.northwestern.edu/netlogo/models/models/Sample%20Models/Biology/Evolution/Bug%20Hunt%20Camouflage.png"

  ; The callback anonymous procedure runs once the data is fetched.  The `setup` procedure
  ; continues execution while that fetch is executed.
  fetch:url-async image-url [ data ->
    import-a:drawing data
    ; Once the image is loaded, we're ready to play, so now we reset
    reset-ticks
  ]
  ; If we ran the `reset-ticks` here, after the fetch, it would probably execute before the image is loaded.
end

to go
  ; Because the **go** button is disabled until ticks are reset, we can be sure the image is loaded
  ; and we're ready to run the real model logic.
end
```

## I noticed that the color values from `import-a:pcolors` are not exactly the same as the values from `import-pcolors` in desktop NetLogo.  Why is that?

While `import-a:pcolors` will give exactly the same results, whether executed in desktop NetLogo or NetLogo Web, it will sometimes give different results than desktop's normal `import-pcolors`.  The reason is that desktop's `import-pcolors` does some things that are both arbitrary and incredibly difficult to reproduce in JavaScript, so the version of the primitive in the desktop NetLogo extension and NetLogo Web extension use code that is simplified and more reproducible.  Across *all* versions and platforms, though, the images should still look incredibly similar when imported.  If they don't, it's a bug, and please report it in the "Issues" tab above.

## Building

Open it in SBT.  If you successfully run `package`, `import-a.jar` is created.

## Terms of Use

[![CC0](http://i.creativecommons.org/p/zero/1.0/88x31.png)](http://creativecommons.org/publicdomain/zero/1.0/)

The NetLogo Import-A extension is in the public domain.  To the extent possible under law, Uri Wilensky has waived all copyright and related or neighboring rights.
