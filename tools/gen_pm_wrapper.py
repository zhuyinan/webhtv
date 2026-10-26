#!/usr/bin/env python3
"""Generates SpoofPackageManager.java.

Input 1: file with one method per line: RET|NAME|PARAMS(csv)|THROWS(csv)
Input 2: file with the official release certificate hex (whitelisted by spider jars).
Output:  delegating PackageManager subclass that answers getPackageInfo(<our pkg>, GET_SIGNATURES)
         with the official certificate, and delegates everything else.
"""
import sys

methods_path, sig_path, out_path = sys.argv[1], sys.argv[2], sys.argv[3]
sig_hex = open(sig_path).read().strip()

def simple(t):
    # nested classes of PackageManager resolve via inheritance; dotted outer.Inner does not compile
    prefix = "android.content.pm.PackageManager."
    return t[len(prefix):] if t.startswith(prefix) else t

body = []
body.append("package com.fongmi.android.tv.utils;")
body.append("")
body.append("import android.content.Context;")
body.append("import android.content.pm.PackageInfo;")
body.append("import android.content.pm.PackageManager;")
body.append("import android.content.pm.Signature;")
body.append("")
body.append("import java.lang.reflect.InvocationTargetException;")
body.append("import java.lang.reflect.Method;")
body.append("import java.util.Arrays;")
body.append("")
body.append("/**")
body.append(" * Subscription spider jars whitelist the official release signature and sabotage anything else")
body.append(" * (InitCheck: getPackageInfo(pkg, GET_SIGNATURES) -> md5 whitelist, else empty results +")
body.append(" * \"the API is temporarily not adapted to this app\"). We self-compile the app, so we answer those")
body.append(" * queries with the official certificate and delegate the rest unchanged.")
body.append(" */")
body.append("public class SpoofPackageManager extends PackageManager {")
body.append("")
body.append("    private static final String OFFICIAL_CERT_HEX =")
for i in range(0, len(sig_hex), 100):
    body.append('            "%s"%s' % (sig_hex[i:i+100], ' +' if i + 100 < len(sig_hex) else ';'))
body.append("")
body.append("    private final PackageManager base;")
body.append("    private final String pkg;")
body.append("")
body.append("    public SpoofPackageManager(Context context, PackageManager base) {")
body.append("        this.base = base;")
body.append("        this.pkg = context.getPackageName();")
body.append("    }")
body.append("")
body.append("    @Override")
body.append("    public PackageInfo getPackageInfo(String packageName, int flags) throws NameNotFoundException {")
body.append("        PackageInfo info = base.getPackageInfo(packageName, flags);")
body.append("        if (pkg.equals(packageName) && (flags & GET_SIGNATURES) != 0)")
body.append("            info.signatures = new Signature[]{new Signature(OFFICIAL_CERT_HEX)};")
body.append("        return info;")
body.append("    }")
body.append("")
PRIMS = {'int': 'Integer', 'long': 'Long', 'boolean': 'Boolean', 'float': 'Float',
         'double': 'Double', 'short': 'Short', 'byte': 'Byte', 'char': 'Character'}

def cls_literal(t):
    prim = {'int', 'long', 'boolean', 'float', 'double', 'short', 'byte', 'char'}
    if t in prim:
        return t + '.class'
    while t.endswith('[]'):
        return cls_literal(t[:-2]) + '[]' if False else cls_literal_base(t)
    return cls_literal_base(t)

def cls_literal_base(t):
    prefix = 'android.content.pm.PackageManager.'
    simple = t[len(prefix):] if t.startswith(prefix) else t
    return simple + '.class'

def erase(t):
    return t.split('<')[0] if '<' in t else t

for line in open(methods_path):
    line = line.strip()
    if not line:
        continue
    ret, name, params, throws = line.split('|')
    ret = simple(ret)
    params = [simple(x) for x in params.split(',') if x]
    if name == 'getPackageInfo' and params == ['java.lang.String', 'int']:
        continue  # hand-written above
    args = ', '.join('a%d' % i for i in range(len(params)))
    plist_s = ', '.join('final %s a%d' % (p, i) for i, p in enumerate(params))
    types_s = 'new Class[]{%s}' % ', '.join(cls_literal(p) for p in params) if params else 'new Class<?>[0]'
    call = 'invoke("%s", %s%s)' % (name, types_s, (', ' + args) if args else '')
    throws_s = ' throws ' + throws if throws else ''
    body.append("    @Override")
    body.append("    public %s %s(%s)%s {" % (ret, name, plist_s, throws_s))
    if ret == 'void':
        body.append("        %s;" % call)
    elif ret in PRIMS:
        body.append("        return (%s) %s;" % (PRIMS[ret], call))
    else:
        body.append("        return (%s) %s;" % (erase(ret), call))
    body.append("    }")
    body.append("")

body.append("    PackageManager base() { return base; }")
body.append("")
body.append("    private Object invoke(String name, Class<?>[] types, Object... args) {")
body.append("        try {")
body.append("            return base.getClass().getMethod(name, types).invoke(base, args);")
body.append("        } catch (java.lang.reflect.InvocationTargetException e) {")
body.append("            Throwable cause = e.getCause() == null ? e : e.getCause();")
body.append("            if (cause instanceof RuntimeException) throw (RuntimeException) cause;")
body.append("            if (cause instanceof Error) throw (Error) cause;")
body.append("            throw new RuntimeException(cause);")
body.append("        } catch (ReflectiveOperationException e) {")
body.append("            throw new RuntimeException(e);")
body.append("        }")
body.append("    }")

body.append("}")
open(out_path, 'w').write('\n'.join(body) + '\n')
print("generated", out_path)
