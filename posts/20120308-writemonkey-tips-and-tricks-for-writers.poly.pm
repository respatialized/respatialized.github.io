#lang pollen

◊(define-meta title "WriteMonkey: Tips and Tricks for Writers")
◊(define-meta published "2012-03-08")
◊(define-meta tags "markdown,writemonkey,text-editing")

I use WriteMonkey for almost all my writing. It’s the best Windows-based text editor I have found for writing prose (as opposed to programming code).

WriteMonkey is extremely ◊link["http://daringfireball.net/projects/markdown/"]{Markdown}-friendly — useful if, for example, like me you ◊link["http://notely.blogspot.com/2011/08/how-to-use-markdown-in-blogspot-posts.html"]{write all your blog posts in Markdown format}.

Not all of WriteMonkey’s features are well-explained or documented, so I’m writing them up here.

◊h3[#:id "configure-markdown-features"]{Configure Markdown features}

◊ul{
◊li{Markdown highlighting will not work unless you have Markdown set as your “Markup Standard” — set this in the ◊code{Print & Export} section of your Preferences screen.}
◊li{Set the font size/weight/style used on headers: in the Preferences screen’s ◊code{Colors & Fonts} tab, click the button labeled ◊code{...} in the upper right section (why they didn’t label it more clearly is beyond my understanding).}
◊li{Make your exports look great. ◊link["https://docs.google.com/open?id=0B9SDJ22NRBkrTTVhYkcwMVVSTGVMQkc0QWtCcXdsUQ"]{Download the template in this zip file} and place it in WriteMonkey’s ◊code{templates} folder. It’s a version of ◊link["http://kevinburke.bitbucket.org/markdowncss"]{this Markdown stylesheet} with the following changes:

◊ul{
◊li{Removed ◊code{padding: 0; margin: 0} rule for the ◊code{ul} and ◊code{ol} elements - this preserves indentation in multi-level lists.}
◊li{The ◊code{max-width} was widened to look better on bigger screens.}
}}
}

◊h3[#:id "some-undocumented-features-i-found-by-accident"]{Some undocumented features I found by accident}

◊ul{
◊li{You can toggle whether WM will use normal quotes or “smart quotes” with ◊code{CTRL+SHIFT+'} (apostrophe).}
◊li{Out of the box: type ◊code{/now} to insert the timestamp. You can format this timestamp in the Preferences screen.}
}

◊h3[#:id "use-writemonkey-to-write-your-book"]{Use WriteMonkey to write your book}

WriteMonkey has a number of great features for writers:

◊ul{
◊li{It lets you set and monitor progress goals for your writing based on either word count or time or both.}
◊li{Hit ◊code{F5} to toggle between your main text and the “repository,” which works as kind of a scratch pad for the current file.}
◊li{You can use the Jump screen to set navigate around your text’s headings, bookmarks, and todo items.}
}

The upcoming version (2.3.5.0 as of this writing), however, will have some great project management functionality. (See ◊link["http://writemonkey.com/new.php"]{here} for more info)

◊ul{
◊li{Folders will be treated as projects, and all the files within it will be part of the project. You’ll be able to switch quickly between text files in the same folder using a new Files view in the Jumps window.}
◊li{You’ll be able to quickly merge all of a project’s files into a single text file.}
◊li{You’ll be able to mark a file with “tags” using a comment line (starting with ◊code{\\}) at the top of the file, and filter the project file list by tags.}
◊li{Special tags affect how the file is treated in the project window

◊ul{
◊li{Tagging a file with a color name will cause that file to show up with a colored star in the jump screen. Multple colors mean multiples stars, e.g. ◊code{// red red red} will add three red stars.}
◊li{Adding the “draft” tag will move the file to the “repository section” — the file will be presented with lighter color and excluded from total word count.}
◊li{Tag with a percentage, e.g. ◊code{// 50%} to add a grey progress bar}
◊li{Tag with a date in order to add a deadline; the border of the file will turn red when it becomes past-due}
}}
}

Let us know of any additional tips in the comments!

◊h2[#:id "comments"]{Comments}

◊h3[#:id "kabi-park-said"]{◊link["https://www.blogger.com/profile/06871116148520330540"]{Kabi Park} said:}

I like writing using ◊link["https://draftin.com/"]{Draft} which is online editor and supporting markdown &amp; version control instead of WriteMonkey.

(Comment posted April 02, 2013)

◊h3[#:id "apps4all-said"]{◊link["https://www.blogger.com/profile/02574603827688030109"]{apps4all} said:}

You should have a look at SmartDown : ◊link["http://www.aflava.com/"]{More infos} which provides a zen UI and advanced features like focus mode or is able to “fold” markdown

(Comment posted November 11, 2014)