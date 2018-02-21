# Patches
  * `autoconf/texinfo.patch` has been retrieved from https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=711297 and fixes
        
        autoconf.texi:8017: misplaced }
        autoconf.texi:8018: must be after `@defmac' to use `@defmacx'
        autoconf.texi:8019: misplaced }
        autoconf.texi:8206: must be after `@defmac' to use `@defmacx'
        autoconf.texi:8271: misplaced }
        autoconf.texi:8290: misplaced }
        autoconf.texi:8317: misplaced }
        autoconf.texi:8380: must be after `@defmac' to use `@defmacx'
        conftest.c:4597: must be after `@defmac' to use `@defmacx'
        conftest.c:15929: must be after `@defmac' to use `@defmacx'
        make[3]: *** [autoconf.html] Error 1
        make[3]: Leaving directory `/tmp/buildd/autoconf-2.69/doc'
        make[2]: *** [html-recursive] Error 1
        make[2]: Leaving directory `/tmp/buildd/autoconf-2.69'
        make[1]: *** [override_dh_auto_build] Error 2
        make[1]: Leaving directory `/tmp/buildd/autoconf-2.69'
        make: *** [build] Error 2

    in `autoconf` 2.69