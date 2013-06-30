HighlightingTest = TestCase("HighlightingTest");

HighlightingTest.prototype.testHighlighting = function() {
    var colored = tokenColoring("0123456789", ["a identifier", "b", "c identifier", "d"], [4, 1, 4, 1]);
    //                           aaaabccccd
    assertEquals('<table><tr><td class="unselectable">1</td><td><span id="p0" class="a identifier" jpt30pos="0">0123</span><span id="p4" class="b" jpt30pos="4">4</span><span id="p5" class="c identifier" jpt30pos="5">5678</span><span id="p9" class="d" jpt30pos="9">9</span></td></tr></table>', colored);

    $(document).find('body').append("<div id='code'></div>");
    var scratch = $("#code");
    scratch.empty();
    scratch.append(colored);
    addHighlights([[0, 4], [9, 10]]); //exclusive ends, must be synchronized with the server
    assertEquals('<table><tbody><tr><td class="unselectable">1</td><td><span id="p0" class="a identifier highlight" jpt30pos="0">0123</span><span id="p4" class="b" jpt30pos="4">4</span><span id="p5" class="c identifier" jpt30pos="5">5678</span><span id="p9" class="d highlight" jpt30pos="9">9</span></td></tr></tbody></table>', scratch.html());
    scratch.empty();
    scratch.append(colored);
    addHighlights([[1, 5], [6, 8]]); //exclusive ends, must be synchronized with the server
    assertEquals('<table><tbody><tr><td class="unselectable">1</td><td><span id="p0" class="a identifier" jpt30pos="0">0</span><span id="p1" class="a identifier highlight" jpt30pos="1">123</span><span id="p4" class="b highlight" jpt30pos="4">4</span><span id="p5" class="c identifier" jpt30pos="5">5</span><span id="p6" class="c identifier highlight" jpt30pos="6">67</span><span id="p8" class="c identifier" jpt30pos="8">8</span><span id="p9" class="d" jpt30pos="9">9</span></td></tr></tbody></table>', scratch.html());
    scratch.empty();
    scratch.append(colored);
    addHighlights([[0, 2], [9, 10]]); //exclusive ends, must be synchronized with the server
    assertEquals('<table><tbody><tr><td class="unselectable">1</td><td><span id="p0" class="a identifier highlight" jpt30pos="0">01</span><span id="p2" class="a identifier" jpt30pos="2">23</span><span id="p4" class="b" jpt30pos="4">4</span><span id="p5" class="c identifier" jpt30pos="5">5678</span><span id="p9" class="d highlight" jpt30pos="9">9</span></td></tr></tbody></table>', scratch.html());
};
