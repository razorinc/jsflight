var reNotWhitespace = /[^\s]/;

var Str = {};

// ************************************************************************************************
// Whitespace and Entity conversions

var entityConversionLists = Str.entityConversionLists =
{
    normal : {
        whitespace : {
            '\t' : '\u200c\u2192',
            '\n' : '\u200c\u00b6',
            '\r' : '\u200c\u00ac',
            ' '  : '\u200c\u00b7'
        }
    },
    reverse : {
        whitespace : {
            '&Tab;' : '\t',
            '&NewLine;' : '\n',
            '\u200c\u2192' : '\t',
            '\u200c\u00b6' : '\n',
            '\u200c\u00ac' : '\r',
            '\u200c\u00b7' : ' '
        }
    }
};

var normal = entityConversionLists.normal,
    reverse = entityConversionLists.reverse;


//************************************************************************************************
// Entity escaping

var entityConversionRegexes =
{
    normal : {},
    reverse : {}
};

var escapeEntitiesRegEx =
{
    normal : function(list)
    {
        var chars = [];
        for ( var ch in list)
        {
            chars.push(ch);
        }
        return new RegExp('([' + chars.join('') + '])', 'gm');
    },
    reverse : function(list)
    {
        var chars = [];
        for ( var ch in list)
        {
            chars.push(ch);
        }
        return new RegExp('(' + chars.join('|') + ')', 'gm');
    }
};

function getEscapeRegexp(direction, lists)
{
    var name = '', re;
    var groups = [].concat(lists);
    for (i = 0; i < groups.length; i++)
    {
        name += groups[i].group;
    }
    re = entityConversionRegexes[direction][name];
    if (!re)
    {
        var list = {};
        if (groups.length > 1)
        {
            for ( var i = 0; i < groups.length; i++)
            {
                var aList = entityConversionLists[direction][groups[i].group];
                for ( var item in aList)
                    list[item] = aList[item];
            }
        }
        else if (groups.length==1)
        {
            list = entityConversionLists[direction][groups[0].group]; // faster for special case
        }
        else
        {
            list = {}; // perhaps should print out an error here?
        }
        re = entityConversionRegexes[direction][name] = escapeEntitiesRegEx[direction](list);
    }
    return re;
}

function createSimpleEscape(name, direction)
{
    return function(value)
    {
        var list = entityConversionLists[direction][name];
        return String(value).replace(
                getEscapeRegexp(direction, {
                    group : name,
                    list : list
                }),
                function(ch)
                {
                    return list[ch];
                }
               );
    }
}

function escapeGroupsForEntities(str, lists)
{
    lists = [].concat(lists);
    var re = getEscapeRegexp('normal', lists),
        split = String(str).split(re),
        len = split.length,
        results = [],
        cur, r, i, ri = 0, l, list, last = '';
    if (!len)
        return [ {
            str : String(str),
            group : '',
            name : ''
        } ];
    for (i = 0; i < len; i++)
    {
        cur = split[i];
        if (cur == '')
            continue;
        for (l = 0; l < lists.length; l++)
        {
            list = lists[l];
            r = entityConversionLists.normal[list.group][cur];
            // if (cur == ' ' && list.group == 'whitespace' && last == ' ') // only show for runs of more than one space
            //     r = ' ';
            if (r)
            {
                results[ri] = {
                    'str' : r,
                    'class' : list['class'],
                    'extra' : list.extra[cur] ? list['class']
                            + list.extra[cur] : ''
                };
                break;
            }
        }
        // last=cur;
        if (!r)
            results[ri] = {
                'str' : cur,
                'class' : '',
                'extra' : ''
            };
        ri++;
    }
    return results;
}

Str.escapeGroupsForEntities = escapeGroupsForEntities;

function unescapeEntities(str, lists)
{
    var re = getEscapeRegexp('reverse', lists),
        split = String(str).split(re),
        len = split.length,
        results = [],
        cur, r, i, ri = 0, l, list;

    if (!len)
        return str;

    lists = [].concat(lists);
    for (i = 0; i < len; i++)
    {
        cur = split[i];
        if (cur == '')
            continue;

        for (l = 0; l < lists.length; l++)
        {
            list = lists[l];
            r = entityConversionLists.reverse[list.group][cur];
            if (r)
            {
                results[ri] = r;
                break;
            }
        }

        if (!r)
            results[ri] = cur;
        ri++;
    }
    return results.join('') || '';
}

// ************************************************************************************************
// String escaping

var escapeForTextNode = Str.escapeForTextNode = createSimpleEscape('text', 'normal');
var escapeForHtmlEditor = Str.escapeForHtmlEditor = createSimpleEscape('editor', 'normal');
var escapeForElementAttribute = Str.escapeForElementAttribute = createSimpleEscape('attributes', 'normal');
var escapeForCss = Str.escapeForCss = createSimpleEscape('css', 'normal');

// deprecated compatibility functions
Str.deprecateEscapeHTML = createSimpleEscape('text', 'normal');
Str.deprecatedUnescapeHTML = createSimpleEscape('text', 'reverse');

var escapeForSourceLine = Str.escapeForSourceLine = createSimpleEscape('text', 'normal');

var unescapeWhitespace = createSimpleEscape('whitespace', 'reverse');

Str.unescapeForTextNode = function(str)
{
    if (Options.get("showTextNodesWithWhitespace"))
        str = unescapeWhitespace(str);

    if (Options.get("entityDisplay") == "names")
        str = escapeForElementAttribute(str);

    return str;
};

Str.unescapeForURL = createSimpleEscape('text', 'reverse');

Str.escapeNewLines = function(value)
{
    return value.replace(/\r/gm, "\\r").replace(/\n/gm, "\\n");
};

Str.stripNewLines = function(value)
{
    return typeof(value) == "string" ? value.replace(/[\r\n]/gm, " ") : value;
};

Str.escapeSingleQuoteJS = function(value)
{
    return value.replace("\\", "\\\\", "g").replace(/\r/gm, "\\r")
                .replace(/\n/gm, "\\n").replace("'", "\\'", "g");
};

Str.reverseString = function(value)
{
    return value.split("").reverse().join("");
};

Str.escapeJS = function(value)
{
    return value.replace("\\", "\\\\", "g").replace(/\r/gm, "\\r")
        .replace(/\n/gm, "\\n").replace('"', '\\"', "g");
};

Str.cropString = function(text, limit, alterText)
{
    if (!alterText)
        alterText = "...";

    // Make sure it's a string.
    text = String(text);

    // Use default limit if necessary.
    if (!limit)
        limit = Options.get("stringCropLength");

    // Crop the string only if a limit is actually specified.
    if (limit <= 0)
        return text;

    var halfLimit = (limit / 2);
    halfLimit -= 2; // adjustment for alterText's increase in size

    if (text.length > limit)
        return text.substr(0, halfLimit) + alterText + text.substr(text.length-halfLimit);

    return text;
};

Str.cropStringEx = function(text, limit, alterText, pivot)
{
    if (!alterText)
        alterText = "...";

    // Make sure it's a string.
    text = String(text);

    // Use default limit if necessary.
    if (!limit)
        limit = Options.get("stringCropLength");

    // Crop the string only if a limit is actually specified.
    if (limit <= 0)
        return text;

    if (text.length < limit)
        return text;

    if (typeof(pivot) == "undefined")
        pivot = text.length / 2;

    var halfLimit = (limit / 2);

    // Adjust the pivot to the real center in case it's at an edge.
    if (pivot < halfLimit)
        pivot = halfLimit;

    if (pivot > text.length - halfLimit)
        pivot = text.length - halfLimit;

    // Get substring around the pivot
    var begin = Math.max(0, pivot - halfLimit);
    var end = Math.min(text.length - 1, pivot + halfLimit);
    var result = text.substring(begin, end);

    // Add alterText to the beginning or end of the result as necessary.
    if (begin > 0)
        result = alterText + result;

    if (end < text.length - 1)
        result += alterText;

    return result;
};

Str.lineBreak = function()
{
    if (System.isWin(window))
        return "\r\n";

    if (System.isMac(window))
        return "\r";

    return "\n";
};

Str.cropMultipleLines = function(text, limit)
{
    return this.escapeNewLines(this.cropString(text, limit));
};

Str.isWhitespace = function(text)
{
    return !reNotWhitespace.exec(text);
};

Str.splitLines = function(text)
{
    if (!text)
        return [];

    var reSplitLines2 = /.*(:?\r\n|\n|\r)?/mg;
    var lines;
    if (text.match)
    {
        lines = text.match(reSplitLines2);
    }
    else
    {
        var str = text+"";
        lines = str.match(reSplitLines2);
    }
    lines.pop();
    return lines;
};

Str.trim = function(text)
{
    return text.replace(/^\s*|\s*$/g, "");
};

Str.trimLeft = function(text)
{
    return text.replace(/^\s+/, "");
};

Str.trimRight = function(text)
{
    return text.replace(/\s+$/, "");
};

Str.hasPrefix = function(hay, needle)
{
    // Passing empty string is ok, but null or undefined is not.
    if (hay == null)
    {
        if (FBTrace.DBG_ERRORS)
            FBTrace.sysout("Str.hasPrefix; string must not be null", {hay: hay, needle: needle});

        return false;
    }

    // This is the fastest way of testing for prefixes - (hay.indexOf(needle) === 0)
    // can be O(|hay|) in the worst case, and (hay.substr(0, needle.length) === needle)
    // unnecessarily creates a new string and might be O(|needle|) in some JavaScript
    // implementations. See the discussion in issue 3071.
    return hay.lastIndexOf(needle, 0) === 0;
};

Str.endsWith = function(str, suffix)
{
    return str.indexOf(suffix, str.length - suffix.length) !== -1;
};

// ********************************************************************************************* //
// HTML Wrap

Str.wrapText = function(text, noEscapeHTML)
{
    var reNonAlphaNumeric = /[^A-Za-z_$0-9'"-]/;

    var html = [];
    var wrapWidth = Options.get("textWrapWidth");

    // Split long text into lines and put every line into a <code> element (only in case
    // if noEscapeHTML is false). This is useful for automatic scrolling when searching
    // within response body (in order to scroll we need an element).
    // Don't use <pre> elements since this adds additional new line endings when copying
    // selected source code using Firefox->Edit->Copy (Ctrl+C) (issue 2093).
    var lines = Str.splitLines(text);
    for (var i = 0; i < lines.length; ++i)
    {
        var line = lines[i];

        if (wrapWidth > 0)
        {
            while (line.length > wrapWidth)
            {
                var m = reNonAlphaNumeric.exec(line.substr(wrapWidth, 100));
                var wrapIndex = wrapWidth + (m ? m.index : 0);
                var subLine = line.substr(0, wrapIndex);
                line = line.substr(wrapIndex);

                if (!noEscapeHTML) html.push("<code class=\"wrappedText focusRow\" role=\"listitem\">");
                html.push(noEscapeHTML ? subLine : escapeForTextNode(subLine));
                if (!noEscapeHTML) html.push("</code>");
            }
        }

        if (!noEscapeHTML) html.push("<code class=\"wrappedText focusRow\" role=\"listitem\">");
        html.push(noEscapeHTML ? line : escapeForTextNode(line));
        if (!noEscapeHTML) html.push("</code>");
    }

    return html;
};

Str.insertWrappedText = function(text, textBox, noEscapeHTML)
{
    var html = Str.wrapText(text, noEscapeHTML);
    textBox.innerHTML = "<pre role=\"list\">" + html.join("") + "</pre>";
};

// ********************************************************************************************* //
// Indent

var reIndent = /^(\s+)/;

function getIndent(line)
{
    var m = reIndent.exec(line);
    return m ? m[0].length : 0;
}

Str.cleanIndentation = function(text)
{
    var lines = Str.splitLines(text);

    var minIndent = -1;
    for (var i = 0; i < lines.length; ++i)
    {
        var line = lines[i];
        var indent = getIndent(line);
        if (minIndent == -1 && line && !Str.isWhitespace(line))
            minIndent = indent;
        if (indent >= minIndent)
            lines[i] = line.substr(minIndent);
    }
    return lines.join("");
};

// ********************************************************************************************* //
// Formatting

//deprecated compatibility functions
Str.deprecateEscapeHTML = createSimpleEscape("text", "normal");

/**
 * Formats a number with a fixed number of decimal places considering the locale settings
 * @param {Integer} number Number to format
 * @param {Integer} decimals Number of decimal places
 * @returns {String} Formatted number
 */
Str.toFixedLocaleString = function(number, decimals)
{
    // Check whether 'number' is a valid number
    if (isNaN(parseFloat(number)))
        throw new Error("Value '" + number + "' of the 'number' parameter is not a number");

    // Check whether 'decimals' is a valid number
    if (isNaN(parseFloat(decimals)))
        throw new Error("Value '" + decimals + "' of the 'decimals' parameter is not a number");

    var precision = Math.pow(10, decimals);
    var formattedNumber = (Math.round(number * precision) / precision).toLocaleString();
    var decimalMark = (0.1).toLocaleString().match(/\D/);
    var decimalsCount = (formattedNumber.lastIndexOf(decimalMark) == -1) ?
        0 : formattedNumber.length - formattedNumber.lastIndexOf(decimalMark) - 1;

    // Append decimals if needed
    if (decimalsCount < decimals)
    {
        // If the number doesn't have any decimals, add the decimal mark
        if (decimalsCount == 0)
            formattedNumber += decimalMark;

        // Append additional decimals
        for (var i=0, count = decimals - decimalsCount; i<count; ++i)
            formattedNumber += "0";
    }

    return formattedNumber;
};

// xxxsz: May be refactored when Firefox implements the ECMAScript Internationalization API
// See https://bugzil.la/853301
Str.formatNumber = function(number) { return number.toLocaleString(); };

Str.formatSize = function(bytes)
{
    var negative = (bytes < 0);
    bytes = Math.abs(bytes);

    var sizePrecision = Options.get("sizePrecision");
    if (typeof(sizePrecision) == "undefined")
    {
        Options.set("sizePrecision", 2);
        sizePrecision = 2;
    }

    // Get size precision (number of decimal places from the preferences)
    // and make sure it's within limits.
    sizePrecision = (sizePrecision > 2) ? 2 : sizePrecision;
    sizePrecision = (sizePrecision < -1) ? -1 : sizePrecision;

    var result;

    if (sizePrecision == -1)
        result = bytes + " B";

    var precision = Math.pow(10, sizePrecision);

    if (bytes == -1 || bytes == undefined)
        return "?";
    else if (bytes == 0)
        return "0 B";
    else if (bytes < 1024)
        result = bytes.toLocaleString() + " B";
    else if (Math.round(bytes / 1024 * precision) / precision < 1024)
        result = this.toFixedLocaleString(bytes / 1024, sizePrecision) + " KB";
    else
        result = this.toFixedLocaleString(bytes / (1024 * 1024), sizePrecision) + " MB";

    return negative ? "-" + result : result;
};

/**
 * Returns a formatted time string
 *
 * Examples:
 * Str.formatTime(12345678) => default formatting options => "3h 25m 45.678s"
 * Str.formatTime(12345678, "ms") => use milliseconds as min. time unit => "3h 25m 45s 678ms"
 * Str.formatTime(12345678, null, "m") => use minutes as max. time unit => "205m 45.678s"
 * Str.formatTime(12345678, "m", "h") => use minutes as min. and hours as max. time unit
 *     => "3h 25.7613m"
 *
 * @param {Integer} time Time to format in milliseconds
 * @param {Integer} [minTimeUnit=1] Minimal time unit to use in the formatted string
 *     (default is seconds)
 * @param {Integer} [maxTimeUnit=4] Maximal time unit to use in the formatted string
 *     (default is days)
 * @returns {String} Formatted time string
 */
Str.formatTime = function(time, minTimeUnit, maxTimeUnit, decimalPlaces)
{
    var time = parseInt(time);

    if (isNaN(time))
        return "";

    var timeUnits = [
        {
            unit: "ms",
            interval: 1000
        },
        {
            unit: "s",
            interval: 60
        },
        {
            unit: "m",
            interval: 60
        },
        {
            unit: "h",
            interval: 24
        },
        {
            unit: "d",
            interval: 1
        }
    ];

    if (time == -1)
    {
        return "";
    }
    else
    {
        // Get the index of the min. and max. time unit and the decimal places
        var minTimeUnitIndex = (Math.abs(time) < 1000) ? 0 : 1;
        var maxTimeUnitIndex = timeUnits.length - 1;

        for (var i=0, len=timeUnits.length; i<len; ++i)
        {
            if (timeUnits[i].unit == minTimeUnit)
                minTimeUnitIndex = i;
            if (timeUnits[i].unit == maxTimeUnit)
                maxTimeUnitIndex = i;
        }

        if (!decimalPlaces)
            decimalPlaces = (Math.abs(time) >= 60000 && minTimeUnitIndex == 1 ? 0 : 2);

        // Calculate the maximal time interval
        var timeUnitInterval = 1;
        for (var i=0; i<maxTimeUnitIndex; ++i)
            timeUnitInterval *= timeUnits[i].interval;

        var formattedString = (time < 0 ? "-" : "");
        time = Math.abs(time);
        for (var i=maxTimeUnitIndex; i>=minTimeUnitIndex; --i)
        {
            var value = time / timeUnitInterval;
            if (i != minTimeUnitIndex)
            {
                if (value < 0)
                    value = Math.ceil(value);
                else
                    value = Math.floor(value);
            }
            else
            {
                var decimalFactor = Math.pow(10, decimalPlaces);
                value = Math.round(value * decimalFactor) / decimalFactor;
            }

            if (value != 0 || (i == minTimeUnitIndex && formattedString == ""))
                formattedString += value.toLocaleString() + timeUnits[i].unit + " ";
            time %= timeUnitInterval;
            if (i != 0)
                timeUnitInterval /= timeUnits[i - 1].interval;
        }

        return formattedString.trim();
    }
};

/**
 * Formats an IPv4 or IPv6 address incl. port
 * @param {String} address IP address to format
 * @param {String} [port] IP port to format
 * @returns {String} Formatted IP address
 */
Str.formatIP = function(address, port)
{
    if (!address || address == "")
        return "";

    var result = address;
    var isIPv6Address = address.indexOf(":") != -1;
    if (isIPv6Address)
        result = "["+result+"]";

    if (port && port != "")
        result += ":"+port;

    return result;
};

/**
 * Capitalizes the first letter of a string or each word in it
 *
 * @param {String} string String to format
 * @param {Boolean} [capitalizeEachWord=false] If true, the first character of each word will be
 *     transformed to uppercase, otherwise only the very first character of the string
 * @param {Boolean} [restToLowerCase=true] If true, the rest of the string will be transformed
 *     to lower case, otherwise it will stay untouched
 * @returns {String} Converted string
 */
Str.capitalize = function(string, capitalizeEachWord, restToLowerCase)
{
    function capitalizeFirstLetter(string)
    {
        var rest = string.slice(1);

        if (restToLowerCase !== false)
            rest = rest.toLowerCase();

        return string.charAt(0).toUpperCase() + rest;
    }

    if (!capitalizeEachWord)
        return capitalizeFirstLetter(string, restToLowerCase);

    return string.split(" ").map(capitalizeFirstLetter).join(" ");
};

// ********************************************************************************************* //

Str.safeToString = function(ob)
{
    try
    {
        if (!ob)
        {
            if (ob == undefined)
                return 'undefined';
            if (ob == null)
                return 'null';
            if (ob == false)
                return 'false';
            return "";
        }
        if (ob && (typeof (ob['toString']) == "function") )
            return ob.toString();
        if (ob && typeof (ob['toSource']) == 'function')
            return ob.toSource();
       /* https://bugzilla.mozilla.org/show_bug.cgi?id=522590 */
        var str = "[";
        for (var p in ob)
            str += p+',';
        return str + ']';

    }
    catch (exc)
    {
    	console.log("safeToString FAILS "+exc, exc);
    }
    return "[unsupported: no toString() function in type "+typeof(ob)+"]";
};