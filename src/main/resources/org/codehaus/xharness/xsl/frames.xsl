<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
    xmlns:lxslt="http://xml.apache.org/xslt"
    xmlns:redirect="http://xml.apache.org/xalan/redirect"
    extension-element-prefixes="redirect">
<xsl:output method="html" indent="yes" encoding="US-ASCII"/>
<xsl:decimal-format decimal-separator="." grouping-separator=","/>

<!--
 Copyright 2006 IONA Technologies

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 -->

<xsl:param name="output.dir" select="'.'"/>


<xsl:template match="results">
    <!-- create the index.html -->
    <redirect:write file="{$output.dir}/index.html">
        <xsl:call-template name="index.html"/>
    </redirect:write>

    <!-- create the stylesheet.css -->
    <redirect:write file="{$output.dir}/stylesheet.css">
        <xsl:call-template name="stylesheet.css"/>
    </redirect:write>

    <!-- create the output.js -->
    <redirect:write file="{$output.dir}/output.js">
        <xsl:call-template name="output.js"/>
    </redirect:write>

    <!-- create the overview-summary.html at the root -->
    <redirect:write file="{$output.dir}/overview-summary.html">
        <xsl:apply-templates select="." mode="overview.summary"/>
    </redirect:write>

    <!-- create the all-groups-frame.html at the root -->
    <redirect:write file="{$output.dir}/all-groups-frame.html">
        <xsl:apply-templates select="." mode="all.groups"/>
    </redirect:write>
    
    <!-- create the all-tests-frame.html at the root -->
    <redirect:write file="{$output.dir}/all-tests-frame.html">
        <xsl:apply-templates select="." mode="all.tests.and.services"/>
    </redirect:write>
    
    <!-- process all elements -->
    <xsl:apply-templates select="*"/>
</xsl:template>


<xsl:template name="index.html">
<html>
    <head>
        <title>XHarness Test Results</title>
    </head>
    <frameset cols="20%,80%">
        <frameset rows="30%,70%">
            <frame src="all-groups-frame.html" name="groupListFrame"/>
            <frame src="all-tests-frame.html" name="testListFrame"/>
        </frameset>
        <frame src="overview-summary.html" name="mainFrame"/>
        <noframes>
            <h2>Frame Alert</h2>
            <p>
                This document is designed to be viewed using the frames feature. If you see this message, you are using a non-frame-capable web client.
            </p>
        </noframes>
    </frameset>
</html>
</xsl:template>


<!-- this is the stylesheet css to use for nearly everything -->
<xsl:template name="stylesheet.css">
body {
    font:normal 68% verdana,arial,helvetica;
    color:#000000;
}
table tr td, table tr th {
    font-size: 68%;
}
table.details tr th{
    font-weight: bold;
    text-align:left;
    background:#a6caf0;
}
table.details tr td{
    background:#eeeee0;
}

table.details pre{
    margin-top:0em; margin-bottom:0em;
}

p {
    line-height:1.5em;
    margin-top:0.5em; margin-bottom:1.0em;
}
h1 {
    margin: 0px 0px 5px; font: 165% verdana,arial,helvetica
}
h2 {
    margin-top: 1em; margin-bottom: 0.5em; font: bold 125% verdana,arial,helvetica
}
h3 {
    margin-bottom: 0.5em; font: bold 115% verdana,arial,helvetica
}
h4 {
    margin-bottom: 0.5em; font: bold 100% verdana,arial,helvetica
}
h5 {
    margin-bottom: 0.5em; font: bold 100% verdana,arial,helvetica
}
h6 {
    margin-bottom: 0.5em; font: bold 100% verdana,arial,helvetica
}
.Skipped {
    color:blue;
}
.Warning {
    font-weight:bold; color:purple;
}
.Failed {
    font-weight:bold; color:red;
}
.Output {
    font-size: 120%;
}
</xsl:template>

<xsl:template name="output.js">
<xsl:text disable-output-escaping="yes">
var table = document.getElementById("outputTable");
var data = new Array();
for (var i=1; i &lt; table.rows.length; i++) {
  var row = table.rows[i];
  var key = getNodeType(row.cells[0], 3).data;
  var pre = getNodeType(row.cells[1], 1).cloneNode(true);
  data.push(new Array(key, pre));

}
refresh();

function clearTable() {
  var table = document.getElementById("outputTable");
  var elemCount = table.rows.length;
  for (var i=1; i &lt; elemCount; i++) {
    var row = table.rows[1];
    table.deleteRow(1);
  }
}

function getNodeType(node, type) {
  for (var i=0; i &lt; node.childNodes.length; i++) {
    if (node.childNodes[i].nodeType == type) {
      return node.childNodes[i];
    }
  }
}

function refresh() {
  clearTable();
  var index = 1;
  var lastKey;
  var lastTd;
  for (var i=0; i &lt; data.length; i++) {
    var key = data[i][0];
    if (key == lastKey) {
      lastTd.appendChild(data[i][1]);
    } else if(!document.getElementById(key) || document.getElementById(key).checked) {
      var row = table.insertRow(index++);
      row.setAttribute("valign", "top");
      var td1 = document.createElement("td");
      var td1text = document.createTextNode(key);
      td1.appendChild(td1text);
      row.appendChild(td1);
      var td2 = document.createElement("td");
      td2.appendChild(data[i][1].cloneNode(true));
      row.appendChild(td2);
      lastTd = td2;
      lastKey = key;
    }
  }
}
</xsl:text>
</xsl:template>

<xsl:template match="results" mode="overview.summary">
    <html>
        <head>
            <title>XHarness Summary</title>
            <xsl:call-template name="create.stylesheet.link">
                <xsl:with-param name="rel.dir"/>
            </xsl:call-template>
        </head>
        <body>
        <xsl:attribute name="onload">open('all-tests-frame.html','testListFrame')</xsl:attribute>
        <xsl:call-template name="page.header"/>
        <h2>Summary</h2>
        <xsl:variable name="testCount" select="count(test)"/>
        <xsl:variable name="skippedTestsCount" select="count(test[@result = 'Skipped'])"/>
        <xsl:variable name="warningTestCount" select="count(test[@result = 'Warning'])"/>
        <xsl:variable name="failTestCount" select="count(test[@result = 'Failed'])"/>
        <xsl:variable name="testSuccessRate" select="($testCount - $failTestCount - $warningTestCount - $skippedTestsCount) div $testCount"/>
        <xsl:variable name="servicesCount" select="count(service)"/>
        <xsl:variable name="skippedServiceCount" select="0"/>
        <xsl:variable name="warningServiceCount" select="count(service[@result = 'Warning'])"/>
        <xsl:variable name="failServiceCount" select="count(service[@result = 'Failed'])"/>
        <xsl:variable name="serviceSuccessRate" select="($servicesCount - $failServiceCount - $warningServiceCount - $skippedServiceCount) div $servicesCount"/>
        <table class="details" border="0" cellpadding="5" cellspacing="2" width="95%">
        <tr valign="top">
            <th style="background:#ffffff;" width="25%"><xsl:text disable-output-escaping="yes">&amp;</xsl:text>nbsp;</th>
            <th>Total</th>
            <th>Skipped</th>
            <th>Warnings</th>
            <th>Failures</th>
            <th>Success rate</th>
        </tr>
        <tr valign="top">
            <th>Tests</th>
            <td><xsl:value-of select="$testCount"/></td>
            <td><xsl:value-of select="$skippedTestsCount"/></td>
            <td><xsl:value-of select="$warningTestCount"/></td>
            <td><xsl:value-of select="$failTestCount"/></td>
            <td>
                <xsl:call-template name="display-percent">
                    <xsl:with-param name="value" select="$testSuccessRate"/>
                </xsl:call-template>
            </td>
        </tr>
        <tr valign="top">
            <th>Services</th>
            <td><xsl:value-of select="$servicesCount"/></td>
            <td><xsl:value-of select="$skippedServiceCount"/></td>
            <td><xsl:value-of select="$warningServiceCount"/></td>
            <td><xsl:value-of select="$failServiceCount"/></td>
            <td>
                <xsl:call-template name="display-percent">
                    <xsl:with-param name="value" select="$serviceSuccessRate"/>
                </xsl:call-template>
            </td>
        </tr>
        </table>
        <table border="0" width="95%">
        <tr>
        <!--td style="text-align: justify;">
        Note: <em>failures</em> are anticipated and checked for with assertions while <em>errors</em> are unanticipated.
        </td-->
        </tr>
        </table>
        
        <xsl:if test="not(/results/@rawdata = '')">
            <table width="100%">
            <tr>
                <td align="left">
                    <a href="{/results/@rawdata}">[Raw Output]</a>
                </td>
            </tr>
            </table>
        </xsl:if>

        <h2>Test Groups</h2>
        <table class="details" border="0" cellpadding="5" cellspacing="2" width="95%">
            <tr valign="top">
                <th width="80%">Name</th>
                <th>Tasks</th>
                <th>Tests</th>
                <th>Services</th>
                <th>Skipped</th>
                <th>Warnings</th>
                <th>Failures</th>
                <th nowrap="nowrap">Time(s)</th>
            </tr>
            <xsl:for-each select="/results/group | /results/xharness">
              <xsl:sort select="@orderid" order="ascending" data-type="number"/>
              <!-- get the node set containing all results that have the same package -->
              <xsl:variable name="allchildren" select="/results/*[@parent = current()/@fullname]"/>
              <xsl:variable name="tasks" select="/results/task[@parent = current()/@fullname]"/>
              <xsl:variable name="verifys" select="/results/verify[@parent = current()/@fullname]"/>
              <xsl:variable name="tests" select="/results/test[@parent = current()/@fullname]"/>
              <xsl:variable name="services" select="/results/service[@parent = current()/@fullname]"/>
              <xsl:variable name="links" select="/results/link[@parent = current()/@fullname]"/>
              <xsl:variable name="subgroups" select="/results/group[@parent = current()/@fullname]"/>
              <xsl:variable name="nongroups" select="$tasks | $verifys | $tests | $services | $links"/>
              <xsl:if test="count($nongroups) &gt; 0">
                <tr valign="top">
                    <xsl:attribute name="class">
                        <xsl:choose>
                            <xsl:when test="@result = 'Failed'">Failed</xsl:when>
                            <xsl:when test="@result = 'Warning'">Warning</xsl:when>
                            <xsl:otherwise>Passed</xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <td>
                        <xsl:choose>
                            <xsl:when test="@fullname = ''">
                                <a href="summary.html"><xsl:text>&lt;root&gt;</xsl:text></a>
                            </xsl:when>
                            <xsl:otherwise>
                                <a href="{@fullname}/summary.html"><xsl:value-of select="@fullname"/></a>
                            </xsl:otherwise>
                        </xsl:choose>
                    </td>
                    <td><xsl:value-of select="count($tasks | $verifys)"/></td>
                    <td><xsl:value-of select="count($tests)"/></td>
                    <td><xsl:value-of select="count($services)"/></td>
                    <td>
                       <xsl:attribute name="class">
                            <xsl:choose>
                                <xsl:when test="count($nongroups[@result = 'Skipped']) &gt; 0">Skipped</xsl:when>
                                <xsl:otherwise>Passed</xsl:otherwise>
                            </xsl:choose>
                        </xsl:attribute>
                        <xsl:value-of select="count($nongroups[@result = 'Skipped'])"/>
                    </td>
                    <td>
                        <xsl:value-of select="count($nongroups[@result = 'Warning'])"/>
                    </td>
                    <td>
                        <xsl:value-of select="count($nongroups[@result = 'Failed'])"/>
                    </td>
                    <td>
                    <xsl:call-template name="display-time">
                        <xsl:with-param name="value" select="sum($nongroups/@time)"/>
                    </xsl:call-template>
                    </td>
                </tr>
              </xsl:if>
            </xsl:for-each>
        </table>
        </body>
        </html>
</xsl:template>


<!--
    Creates an html file that contains a link to all summary.html files on
    each package existing on results.
    @bug there will be a problem here, I don't know yet how to handle unnamed package :(
-->
<xsl:template match="results" mode="all.groups">
    <html>
        <head>
            <title>All Groups</title>
            <xsl:call-template name="create.stylesheet.link">
                <xsl:with-param name="rel.dir"/>
            </xsl:call-template>
        </head>
        <body>
            <h2><a href="overview-summary.html" target="mainFrame">Home</a></h2>
            <h2>All Test Groups</h2>
            <table width="100%">
              <xsl:for-each select="/results/group | /results/xharness">
                <xsl:sort select="@orderid" order="ascending" data-type="number"/>
                <xsl:variable name="allchildren" select="/results/*[@parent = current()/@fullname]"/>
                <xsl:variable name="subgroups" select="/results/group[@parent = current()/@fullname]"/>
                <xsl:if test="count($allchildren) - count($subgroups) &gt; 0">
                  <tr>
                      <td nowrap="nowrap">
                          <xsl:choose>
                              <xsl:when test="@fullname = ''">
                                  <a href="summary.html" target="mainFrame"><xsl:text>&lt;root&gt;</xsl:text></a>
                              </xsl:when>
                              <xsl:otherwise>
                                  <a href="{@fullname}/summary.html" target="mainFrame"><xsl:value-of select="@fullname"/></a>
                              </xsl:otherwise>
                          </xsl:choose>
                      </td>
                  </tr>
                </xsl:if>
              </xsl:for-each>
            </table>
        </body>
    </html>
</xsl:template>


<!--
    Creates an all-classes.html file that contains a link to all summary.html
    on each class.
-->
<xsl:template match="results" mode="all.tests.and.services">
    <html>
        <head>
            <title>All Tests and Services</title>
            <xsl:call-template name="create.stylesheet.link">
                <xsl:with-param name="rel.dir"/>
            </xsl:call-template>
        </head>
        <body>
            <h2>All Tests and Services</h2>
            <h3>Services</h3>
            <table width="100%">
                <xsl:apply-templates select="service" mode="list.all">
                  <xsl:sort select="@orderid" order="ascending" data-type="number"/>
                </xsl:apply-templates>
            </table>
            <h3>Tests</h3>
            <table width="100%">
                <xsl:apply-templates select="test" mode="list.all">
                  <xsl:sort select="@orderid" order="ascending" data-type="number"/>
                </xsl:apply-templates>
            </table>
        </body>
    </html>
</xsl:template>

<xsl:template match="service" mode="list.all">
    <tr>
        <td nowrap="nowrap">
            <a href="{@fullname}/summary.html" target="mainFrame"><xsl:value-of select="@name"/></a>
        </td>
    </tr>
</xsl:template>

<xsl:template match="test" mode="list.all">
    <tr>
        <td nowrap="nowrap">
            <a href="{@fullname}/summary.html" target="mainFrame"><xsl:value-of select="@name"/></a>
        </td>
    </tr>
</xsl:template>

<xsl:template match="xharness">
    <redirect:write file="{$output.dir}/{@fullname}/group-frame.html">`
        <xsl:call-template name="group.overview"/>
    </redirect:write>
    
    <!-- create a summary.html in the group directory -->
    <redirect:write file="{$output.dir}/{@fullname}/summary.html">
        <xsl:call-template name="group.summary">
            <xsl:with-param name="type">Testgroup</xsl:with-param>
            <xsl:with-param name="text"><xsl:text>: </xsl:text><xsl:value-of select="@name"/></xsl:with-param>
        </xsl:call-template>
    </redirect:write>
</xsl:template>

<xsl:template match="group">
    <redirect:write file="{$output.dir}/{@fullname}/group-frame.html">`
        <xsl:call-template name="group.overview"/>
    </redirect:write>
    
    <!-- create a summary.html in the group directory -->
    <redirect:write file="{$output.dir}/{@fullname}/summary.html">
        <xsl:call-template name="group.summary">
            <xsl:with-param name="type">Testgroup</xsl:with-param>
            <xsl:with-param name="text"><xsl:text>: </xsl:text><xsl:value-of select="@name"/></xsl:with-param>
        </xsl:call-template>
    </redirect:write>
</xsl:template>

<xsl:template match="test">
    <redirect:write file="{$output.dir}/{@fullname}/summary.html">
        <xsl:call-template name="group.summary">
            <xsl:with-param name="type">Testcase</xsl:with-param>
            <xsl:with-param name="text"><xsl:text>: </xsl:text><xsl:value-of select="@name"/></xsl:with-param>
        </xsl:call-template>
    </redirect:write>
</xsl:template>

<xsl:template match="service">
    <redirect:write file="{$output.dir}/{@fullname}/summary.html">
        <xsl:call-template name="service.summary"/>
    </redirect:write>
</xsl:template>

<xsl:template match="start">
    <redirect:write file="{$output.dir}/{@fullname}/summary.html">
        <xsl:call-template name="group.summary">
            <xsl:with-param name="type">Start Service</xsl:with-param>
        </xsl:call-template>
    </redirect:write>
</xsl:template>

<xsl:template match="stop">
    <redirect:write file="{$output.dir}/{@fullname}/summary.html">
        <xsl:call-template name="group.summary">
            <xsl:with-param name="type">Stop Service</xsl:with-param>
        </xsl:call-template>
    </redirect:write>
</xsl:template>

<xsl:template match="verify">
    <redirect:write file="{$output.dir}/{@fullname}/summary.html">
        <xsl:call-template name="group.summary">
            <xsl:with-param name="type">Verify Service</xsl:with-param>
        </xsl:call-template>
    </redirect:write>
</xsl:template>

<xsl:template match="task">
    <redirect:write file="{$output.dir}/{@parent}/{@name}.html">
        <xsl:call-template name="task.summary"/>
    </redirect:write>
</xsl:template>

<xsl:template name="group.overview">
    <html>
        <head>
            <xsl:call-template name="create.stylesheet.link">
                <xsl:with-param name="rel.dir" select="@fullname"/>
            </xsl:call-template>
        </head>
        <body>
            <h2>
                <a href="summary.html" target="mainFrame">
                    <xsl:call-template name="display.link">
                        <xsl:with-param name="link"><xsl:value-of select="@fullname"/></xsl:with-param>
                    </xsl:call-template>
                </a>
            </h2>
            <h3>Services</h3>
            <table width="100%">
                <xsl:for-each select="/results/service[@parent = current()/@fullname]">
                    <xsl:sort select="@orderid" order="ascending" data-type="number"/>
                    <tr>
                        <td nowrap="nowrap">
                            <a href="{@name}/summary.html" target="mainFrame"><xsl:value-of select="@name"/></a>
                        </td>
                    </tr>
                </xsl:for-each>
            </table>
            
            <h3>Tests</h3>
            <table width="100%">
                <xsl:for-each select="/results/test[@parent = current()/@fullname]">
                    <xsl:sort select="@orderid" order="ascending" data-type="number"/>
                    <tr>
                        <td nowrap="nowrap">
                            <a href="{@name}/summary.html" target="mainFrame"><xsl:value-of select="@name"/></a>
                        </td>
                    </tr>
                </xsl:for-each>
            </table>
        </body>
    </html>
</xsl:template>


<xsl:template name="group.summary">
    <xsl:param name="type"/>
    <xsl:param name="text"/>
    <html>
        <head>
            <xsl:call-template name="create.stylesheet.link">
                <xsl:with-param name="rel.dir" select="@fullname"/>
            </xsl:call-template>
        </head>
        <body>
            <xsl:if test="$type = 'Testgroup'">
                <xsl:attribute name="onload">open('group-frame.html','testListFrame')</xsl:attribute>
            </xsl:if>
            <xsl:call-template name="page.header"/>
            <h3><xsl:value-of select="$type"/><xsl:value-of select="$text"/></h3>
            <xsl:call-template name="summary.table"/>
            <xsl:variable name="allchildren" select="/results/*[@parent = current()/@fullname]"/>
            <xsl:if test="count($allchildren) &gt; 0">
               <h2>Tasks</h2>
                <p>
                <table class="details" border="0" cellpadding="5" cellspacing="2" width="95%">
                    <xsl:call-template name="tasks.list.header"/>
                    <xsl:apply-templates select="$allchildren" mode="group.list">
                      <xsl:sort select="@orderid" order="ascending" data-type="number"/>
                    </xsl:apply-templates>
                </table>
                </p>
            </xsl:if>
            <!--
            <h2>Output</h2>
            <p>
                <xsl:call-template name="output.table"/>
            </p>
            <xsl:call-template name="create.javascript.link">
                <xsl:with-param name="rel.dir" select="@parent"/>
            </xsl:call-template>
            -->
        </body>
    </html>
</xsl:template>

<xsl:template name="service.summary">
    <html>
        <head>
            <xsl:call-template name="create.stylesheet.link">
                <xsl:with-param name="rel.dir" select="@fullname"/>
            </xsl:call-template>
        </head>
        <body>
            <xsl:call-template name="page.header"/>
            <h3>Service: <xsl:value-of select="@name"/></h3>
            <xsl:call-template name="summary.table"/>
            <xsl:variable name="allchildren" select="/results/*[@parent = current()/@fullname] | /results/*[@reference = current()/@fullname]"/>
            <xsl:if test="count($allchildren) &gt; 0">
               <h2>Tasks</h2>
                <p>
                <table class="details" border="0" cellpadding="5" cellspacing="2" width="95%">
                    <xsl:call-template name="service.list.header"/>
                    <xsl:apply-templates select="$allchildren" mode="service.list">
                      <xsl:sort select="@orderid" order="ascending" data-type="number"/>
                      <xsl:with-param name="service" select="@name"/>
                    </xsl:apply-templates>
                </table>
                </p>
            </xsl:if>
            <!--
            <h2>Output</h2>
            <p>
                <xsl:call-template name="output.table"/>
            </p>
            <xsl:call-template name="create.javascript.link">
                <xsl:with-param name="rel.dir" select="@parent"/>
            </xsl:call-template>
            -->
        </body>
    </html>
</xsl:template>

<xsl:template name="task.summary">
    <html>
        <head>
          <title><xsl:value-of select="@name"/></title>
          <xsl:call-template name="create.stylesheet.link">
              <xsl:with-param name="rel.dir" select="@parent"/>
          </xsl:call-template>
        </head>
        <body>
            <xsl:call-template name="page.header"/>  
            <h3>Task: <xsl:value-of select="@name"/></h3>
            <table class="details" border="0" cellpadding="5" cellspacing="2" width="95%">
              <tr valign="top">
                  <th width="10%">Name</th>
                  <td align="left">
                      <xsl:call-template name="display.link">
                          <xsl:with-param name="link"><xsl:value-of select="@fullname"/></xsl:with-param>
                      </xsl:call-template>
                  </td>
              </tr>
              <tr valign="top">
                  <th width="10%">Parent</th>
                  <td align="left">
                      <a>
                          <xsl:attribute name="href">
                              <xsl:if test="/results/start[@fullname = current()/@parent] | /results/stop[@fullname = current()/@parent]">
                                  <xsl:text>../</xsl:text>
                              </xsl:if>
                              <xsl:text>summary.html</xsl:text>
                          </xsl:attribute>
                          <xsl:call-template name="display.link">
                              <xsl:with-param name="link"><xsl:value-of select="@parent"/></xsl:with-param>
                          </xsl:call-template>
                      </a>
                  </td>
              </tr>
              <tr valign="top">
                  <xsl:attribute name="class">
                      <xsl:choose>
                          <xsl:when test="@result = 'Skipped'">Skipped</xsl:when>
                          <xsl:when test="@result = 'Warning'">Warning</xsl:when>
                          <xsl:when test="@result = 'Failed'">Failed</xsl:when>
                          <xsl:otherwise>Pass</xsl:otherwise>
                      </xsl:choose>
                  </xsl:attribute>
                  <th width="10%">Result</th>
                  <td align="left"><xsl:value-of select="@result"/></td>
              </tr>
              <xsl:if test="command">
                  <tr valign="top">
                      <th>Command</th>
                      <td><xsl:value-of select="command"/></td>
                  </tr>
              </xsl:if>
              <xsl:if test="@retval">
                  <tr valign="top">
                      <th nowrap="nowrap">Return value</th>
                      <td><xsl:value-of select="@retval"/></td>
                  </tr>
              </xsl:if>
              <tr valign="top">
                  <th>Summary</th>
                  <td><xsl:call-template name="format.output"><xsl:with-param name="output" select="description"/></xsl:call-template></td>
              </tr>
              <tr valign="top">
                  <th width="10%" nowrap="nowrap">Time</th>
                  <td><xsl:value-of select="@time"/></td>
              </tr>
            </table>
            <h2>Output</h2>
            <p>
                <xsl:call-template name="output.table"/>
            </p>
            <xsl:call-template name="create.javascript.link">
                <xsl:with-param name="rel.dir" select="@parent"/>
            </xsl:call-template>
        </body>
    </html>
</xsl:template>

<xsl:template name="summary.table">
    <table class="details" border="0" cellpadding="5" cellspacing="2" width="95%">
        <tr valign="top">
            <th width="10%" nowrap="nowrap">Name</th>
            <td>
                <xsl:call-template name="display.link">
                    <xsl:with-param name="link"><xsl:value-of select="@fullname"/></xsl:with-param>
                </xsl:call-template>
            </td>
        </tr>
        <tr valign="top">
            <th width="10%" nowrap="nowrap">Parent</th>
            <td>
                <a href="../summary.html">
                    <xsl:call-template name="display.link">
                        <xsl:with-param name="link"><xsl:value-of select="@parent"/></xsl:with-param>
                    </xsl:call-template>
                </a>
            </td>
        </tr>
        <xsl:if test="@reference">
          <tr valign="top">
            <th width="10%" nowrap="nowrap">
                <xsl:choose>
                    <xsl:when test="self::verify">Service</xsl:when>
                    <xsl:otherwise>Reference</xsl:otherwise>
                </xsl:choose>
            </th>
            <td>
              <a>
                <xsl:attribute name="href">
                  <xsl:call-template name="path"><xsl:with-param name="path" select="@fullname"/></xsl:call-template>
                  <xsl:value-of select="@reference"/>
                  <xsl:text>/summary.html</xsl:text>
                </xsl:attribute>
                <xsl:value-of select="@reference"/></a>
            </td>
          </tr>
        </xsl:if>
        <tr valign="top">
            <xsl:attribute name="class">
                <xsl:choose>
                    <xsl:when test="@result = 'Skipped'">Skipped</xsl:when>
                    <xsl:when test="@result = 'Warning'">Warning</xsl:when>
                    <xsl:when test="@result = 'Failed'">Failed</xsl:when>
                    <xsl:otherwise>Pass</xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <th width="10%">Result</th>
            <td align="left"><xsl:value-of select="@result"/></td>
        </tr>
        <tr valign="top">
            <th>Summary</th>
            <td><xsl:call-template name="format.output"><xsl:with-param name="output" select="description"/></xsl:call-template></td>
        </tr>
        <tr valign="top">
            <th width="10%" nowrap="nowrap">Time</th>
            <td><xsl:value-of select="@time"/></td>
        </tr>
        <xsl:if test="@owner">
            <tr valign="top">
              <th width="10%" nowrap="nowrap">Owner</th>
              <td><xsl:value-of select="@owner"/></td>
            </tr>
        </xsl:if>
    </table>
</xsl:template>

<xsl:template name="output.table">
    <xsl:variable name="stdOutCount"  select="count(output[@level = '0'])"/>
    <xsl:variable name="stdErrCount"  select="count(output[@level = '1'])"/>
    <xsl:variable name="errorCount"   select="count(output[@level = '2'])"/>
    <xsl:variable name="warningCount" select="count(output[@level = '3'])"/>
    <xsl:variable name="infoCount"    select="count(output[@level = '4'])"/>
    <xsl:variable name="verboseCount" select="count(output[@level = '5'])"/>
    <xsl:variable name="debugCount"   select="count(output[@level = '6'])"/>
    <xsl:choose>
      <xsl:when test="$stdOutCount &gt; 0">
        <input type="checkbox" id="StdOut" onclick="javascript:refresh()" checked="checked"/>Std Out
      </xsl:when>
      <xsl:otherwise>
        <input type="checkbox" id="StdOut" onclick="javascript:refresh()" disabled="disabled"/>Std Out
      </xsl:otherwise>
    </xsl:choose>
    <xsl:choose>
      <xsl:when test="$stdErrCount &gt; 0">
        <input type="checkbox" id="StdErr" onclick="javascript:refresh()" checked="checked"/>Std Err
      </xsl:when>
      <xsl:otherwise>
        <input type="checkbox" id="StdErr" onclick="javascript:refresh()" disabled="disabled"/>Std Err
      </xsl:otherwise>
    </xsl:choose>
    <xsl:choose>
      <xsl:when test="$errorCount &gt; 0">
        <input type="checkbox" id="Error" onclick="javascript:refresh()" checked="checked"/>Error
      </xsl:when>
      <xsl:otherwise>
        <input type="checkbox" id="Error" onclick="javascript:refresh()" disabled="disabled"/>Error
      </xsl:otherwise>
    </xsl:choose>
    <xsl:choose>
      <xsl:when test="$stdOutCount + $stdErrCount &gt; 0 and $warningCount &gt; 0">
        <input type="checkbox" id="Warning" onclick="javascript:refresh()"/>Warning
      </xsl:when>
      <xsl:when test="$warningCount &gt; 0">
        <input type="checkbox" id="Warning" onclick="javascript:refresh()" checked="checked"/>Warning
      </xsl:when>
      <xsl:otherwise>
        <input type="checkbox" id="Warning" onclick="javascript:refresh()" disabled="disabled"/>Warning
      </xsl:otherwise>
    </xsl:choose>
    <xsl:choose>
      <xsl:when test="$stdOutCount + $stdErrCount &gt; 0 and $infoCount &gt; 0">
        <input type="checkbox" id="Info" onclick="javascript:refresh()"/>Info
      </xsl:when>
      <xsl:when test="$infoCount &gt; 0">
        <input type="checkbox" id="Info" onclick="javascript:refresh()" checked="checked"/>Info
      </xsl:when>
      <xsl:otherwise>
        <input type="checkbox" id="Info" onclick="javascript:refresh()" disabled="disabled"/>Info
      </xsl:otherwise>
    </xsl:choose>
    <xsl:choose>
      <xsl:when test="$stdOutCount + $stdErrCount + $warningCount + $infoCount &gt; 0 and $verboseCount &gt; 0">
        <input type="checkbox" id="Verbose" onclick="javascript:refresh()"/>Verbose
      </xsl:when>
      <xsl:when test="$verboseCount &gt; 0">
        <input type="checkbox" id="Verbose" onclick="javascript:refresh()" checked="checked"/>Verbose
      </xsl:when>
      <xsl:otherwise>
        <input type="checkbox" id="Verbose" onclick="javascript:refresh()" disabled="disabled"/>Verbose
      </xsl:otherwise>
    </xsl:choose>
    <xsl:choose>
      <xsl:when test="$debugCount &gt; 0">
        <input type="checkbox" id="Debug" onclick="javascript:refresh()"/>Debug
      </xsl:when>
      <xsl:otherwise>
        <input type="checkbox" id="Debug" onclick="javascript:refresh()" disabled="disabled"/>Debug
      </xsl:otherwise>
    </xsl:choose>
    <table class="details" border="0" cellpadding="5" cellspacing="2" width="95%" id="outputTable">
      <tr valign="top">
          <th width="10%">Level</th><th>Data</th>
      </tr>
      <xsl:for-each select="output">
        <tr valign="top">
            <td>
              <xsl:choose>
                <xsl:when test="@level = '0'">StdOut</xsl:when>
                <xsl:when test="@level = '1'">StdErr</xsl:when>
                <xsl:when test="@level = '2'">Error</xsl:when>
                <xsl:when test="@level = '3'">Warning</xsl:when>
                <xsl:when test="@level = '4'">Info</xsl:when>
                <xsl:when test="@level = '5'">Verbose</xsl:when>
                <xsl:when test="@level = '6'">Debug</xsl:when>
                <xsl:otherwise><xsl:value-of select="@level"/></xsl:otherwise>
              </xsl:choose>
            </td>
            <td><pre class="Output"><xsl:call-template name="format.output"><xsl:with-param name="output" select="."/><xsl:with-param name="fullname" select="substring-after(../@fullname, '/')"/></xsl:call-template></pre></td>
        </tr>
      </xsl:for-each>
    </table>
</xsl:template>

<xsl:template name="tasks.list.header">
    <tr valign="top">
        <th width="10%" nowrap="nowrap">Type</th>
        <th width="20%">Name</th>
        <th>Summary</th>
        <th width="10%" nowrap="nowrap">Time(s)</th>
        <th width="10%">Result</th>
    </tr>
</xsl:template>

<xsl:template name="service.list.header">
    <tr valign="top">
        <th width="10%" nowrap="nowrap">Type</th>
        <th width="10%" nowrap="nowrap">Service</th>
        <th width="10%">Origin</th>
        <th>Summary</th>
        <th width="10%" nowrap="nowrap">Time(s)</th>
        <th width="10%">Result</th>
    </tr>
</xsl:template>

<xsl:template name="group.list">
    <xsl:param name="type"/>
    <xsl:param name="link"/>
    <tr valign="top">       
        <xsl:attribute name="class">
            <xsl:choose>
                <xsl:when test="@result = 'Skipped'">Skipped</xsl:when>
                <xsl:when test="@result = 'Warning'">Warning</xsl:when>
                <xsl:when test="@result = 'Failed'">Failed</xsl:when>
                <xsl:otherwise>Pass</xsl:otherwise>
            </xsl:choose>
        </xsl:attribute>
        <td><xsl:value-of select="$type"/></td>
        <td><a href="{$link}.html">
            <xsl:choose>
                <xsl:when test="self::link">
                  <xsl:value-of select="substring-after(@name, '_')"/>
                </xsl:when>
                <xsl:when test="$type = 'Verify'">
                  <xsl:value-of select="substring-after(@name, '_')"/>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="@name"/>
                </xsl:otherwise>    
            </xsl:choose>
        </a></td>
        <td><xsl:call-template name="format.output"><xsl:with-param name="output" select="description"/></xsl:call-template></td>
        <td><xsl:value-of select="@time"/></td>
        <td><xsl:apply-templates select="@result"/></td>
    </tr>
</xsl:template>

<xsl:template name="service.list">
    <xsl:param name="type"/>
    <xsl:param name="service"/>
    <xsl:param name="from"/>
    <xsl:param name="link.to"/>
    <xsl:param name="link.from"/>
    <tr valign="top">       
        <xsl:attribute name="class">
            <xsl:choose>
                <xsl:when test="@result = 'Skipped'">Skipped</xsl:when>
                <xsl:when test="@result = 'Warning'">Warning</xsl:when>
                <xsl:when test="@result = 'Failed'">Failed</xsl:when>
                <xsl:otherwise>Pass</xsl:otherwise>
            </xsl:choose>
        </xsl:attribute>
        <td><xsl:value-of select="$type"/></td>
        <td><a href="{$link.to}.html"><xsl:value-of select="$service"/></a></td>
        <td>
            <a href="{$link.from}/Summary.html">
                <xsl:call-template name="display.link">
                    <xsl:with-param name="link"><xsl:value-of select="$from"/></xsl:with-param>
                </xsl:call-template>
            </a>
        </td>
        <td><xsl:call-template name="format.output"><xsl:with-param name="output" select="description"/></xsl:call-template></td>
        <td><xsl:value-of select="@time"/></td>
        <td><xsl:apply-templates select="@result"/></td>
    </tr>
</xsl:template>

<xsl:template match="group" mode="group.list">
    <xsl:call-template name="group.list">
            <xsl:with-param name="type"><xsl:text>Group</xsl:text></xsl:with-param>
            <xsl:with-param name="link"><xsl:value-of select="@name"/><xsl:text>/summary</xsl:text></xsl:with-param>
    </xsl:call-template>
</xsl:template>

<xsl:template match="test" mode="group.list">
    <xsl:call-template name="group.list">
            <xsl:with-param name="type"><xsl:text>Test</xsl:text></xsl:with-param>
            <xsl:with-param name="link"><xsl:value-of select="@name"/><xsl:text>/summary</xsl:text></xsl:with-param>
    </xsl:call-template>
</xsl:template>

<xsl:template match="service" mode="group.list">
    <xsl:call-template name="group.list">
            <xsl:with-param name="type"><xsl:text>ServiceDef</xsl:text></xsl:with-param>
            <xsl:with-param name="link"><xsl:value-of select="@name"/><xsl:text>/summary</xsl:text></xsl:with-param>
    </xsl:call-template>
</xsl:template>

<xsl:template match="start" mode="group.list">
    <xsl:call-template name="group.list">
            <xsl:with-param name="type"><xsl:text>Start</xsl:text></xsl:with-param>
            <xsl:with-param name="link"><xsl:value-of select="@name"/><xsl:text>/summary</xsl:text></xsl:with-param>
    </xsl:call-template>
</xsl:template>

<xsl:template match="verify" mode="group.list">
    <xsl:call-template name="group.list">
            <xsl:with-param name="type"><xsl:text>Verify</xsl:text></xsl:with-param>
            <xsl:with-param name="link"><xsl:value-of select="@name"/><xsl:text>/summary</xsl:text></xsl:with-param>
    </xsl:call-template>
</xsl:template>

<xsl:template match="stop" mode="group.list">
    <xsl:call-template name="group.list">
            <xsl:with-param name="type"><xsl:text>Stop</xsl:text></xsl:with-param>
            <xsl:with-param name="link"><xsl:value-of select="@name"/><xsl:text>/summary</xsl:text></xsl:with-param>
    </xsl:call-template>
</xsl:template>

<xsl:template match="start" mode="service.list">
    <xsl:param name="service" select="'service'"/>
    <xsl:call-template name="service.list">
            <xsl:with-param name="type"><xsl:text>Start</xsl:text></xsl:with-param>
            <xsl:with-param name="service" select="$service"/>
            <xsl:with-param name="from" select="@reference"/>
            <xsl:with-param name="link.to"><xsl:value-of select="@name"/><xsl:text>/summary</xsl:text></xsl:with-param>
            <xsl:with-param name="link.from">
                <xsl:call-template name="path"><xsl:with-param name="path" select="@parent"/></xsl:call-template>
                <xsl:value-of select="@reference"/>
            </xsl:with-param>
    </xsl:call-template>
</xsl:template>

<xsl:template match="verify" mode="service.list">
    <xsl:param name="service" select="'service'"/>
    <xsl:call-template name="service.list">
            <xsl:with-param name="type"><xsl:text>Verify</xsl:text></xsl:with-param>
            <xsl:with-param name="service" select="$service"/>
            <xsl:with-param name="from" select="@parent"/>
            <xsl:with-param name="link.to">
                <xsl:call-template name="path"><xsl:with-param name="path" select="@reference"/></xsl:call-template>
                <xsl:value-of select="@fullname"/>
                <xsl:text>/summary</xsl:text>
            </xsl:with-param>
            <xsl:with-param name="link.from">
                <xsl:call-template name="path"><xsl:with-param name="path" select="@reference"/></xsl:call-template>
                <xsl:value-of select="@parent"/>
            </xsl:with-param>
    </xsl:call-template>
</xsl:template>

<xsl:template match="stop" mode="service.list">
    <xsl:param name="service" select="'service'"/>
    <xsl:call-template name="service.list">
            <xsl:with-param name="type"><xsl:text>Stop</xsl:text></xsl:with-param>
            <xsl:with-param name="service" select="$service"/>
            <xsl:with-param name="from" select="@reference"/>
            <xsl:with-param name="link.to"><xsl:value-of select="@name"/><xsl:text>/summary</xsl:text></xsl:with-param>
            <xsl:with-param name="link.from">
                <xsl:call-template name="path"><xsl:with-param name="path" select="@parent"/></xsl:call-template>
                <xsl:value-of select="@reference"/>
            </xsl:with-param>
    </xsl:call-template>
</xsl:template>

<xsl:template match="link" mode="group.list">
    <xsl:call-template name="group.list">
            <xsl:with-param name="type" select="substring-before(@name, '_')"/>
            <xsl:with-param name="link">
                <xsl:call-template name="path"><xsl:with-param name="path" select="@parent"/></xsl:call-template>
                <xsl:value-of select="@reference"/>
                <xsl:text>/summary</xsl:text>
            </xsl:with-param>
    </xsl:call-template>
</xsl:template>

<xsl:template match="task" mode="group.list">
    <xsl:param name="prefix"/>
    <xsl:call-template name="group.list">
            <xsl:with-param name="type"><xsl:text>Task</xsl:text></xsl:with-param>
            <xsl:with-param name="link"><xsl:value-of select="$prefix"/><xsl:value-of select="@name"/></xsl:with-param>
    </xsl:call-template>
</xsl:template>


<!-- **************************
          UTIL FUNCTIONS
     ************************** -->

<!--
    transform string like a.b.c to ../../../
    @param path the path to transform into a descending directory path
-->
<xsl:template name="path">
    <xsl:param name="path"/>
    <xsl:if test="contains($path,'/')">
        <xsl:text>../</xsl:text>    
        <xsl:call-template name="path">
            <xsl:with-param name="path"><xsl:value-of select="substring-after($path,'/')"/></xsl:with-param>
        </xsl:call-template>    
    </xsl:if>
    <xsl:if test="not(contains($path,'/')) and not($path = '')">
        <xsl:text>../</xsl:text>    
    </xsl:if>
</xsl:template>
    
    
<xsl:template name="display.link">
    <xsl:param name="link"/>
    <xsl:choose>
        <xsl:when test="$link = ''">
            <xsl:text>&lt;root&gt;</xsl:text>
        </xsl:when>
        <xsl:otherwise>
            <xsl:value-of select="$link"/>
        </xsl:otherwise>
    </xsl:choose>
</xsl:template>


<xsl:template name="format.output">
    <xsl:param name="output"/>
    <xsl:param name="fullname" select="@fullname"/>
    <xsl:variable name="temp" select="substring-after($output, '@')"/>
    <xsl:choose>
        <xsl:when test="contains($temp,'@')">
            <xsl:value-of select="substring-before($output, '@')"/>
            <a>
                <xsl:attribute name="href">
                    <xsl:call-template name="path">
                        <xsl:with-param name="path"><xsl:value-of select="$fullname"/></xsl:with-param>
                    </xsl:call-template> 
                    <xsl:value-of select="substring-before($temp, '@')"/><xsl:text>.html</xsl:text>
                </xsl:attribute>
                <xsl:value-of select="substring-before($temp, '@')"/>
            </a>
            <xsl:call-template name="format.output">
                <xsl:with-param name="output" select="substring-after($temp, '@')"/>
                <xsl:with-param name="fullname" select="$fullname"/>
            </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
            <xsl:value-of select="$output"/>
        </xsl:otherwise>
    </xsl:choose>
</xsl:template>


<!-- create the link to the stylesheet based on the package name -->
<xsl:template name="create.stylesheet.link">
    <xsl:param name="rel.dir"/>
    <link rel="stylesheet" type="text/css" title="Style"><xsl:attribute name="href"><xsl:call-template name="path"><xsl:with-param name="path" select="$rel.dir"/></xsl:call-template>stylesheet.css</xsl:attribute></link>
</xsl:template>


<!-- create the link to the javascript based on the package name -->
<xsl:template name="create.javascript.link">
    <xsl:param name="rel.dir"/>
    <script language="javascript"><xsl:attribute name="src"><xsl:call-template name="path"><xsl:with-param name="path" select="$rel.dir"/></xsl:call-template>output.js</xsl:attribute></script>
</xsl:template>


<!-- Page HEADER -->
<xsl:template name="page.header">
    <h1>XHarness Test Results</h1>
    <table width="100%">
    <tr>
        <td align="left"></td>
        <td align="right">Designed for use with <a href="http://xharness.codehaus.org" target="_blank">XHarness</a> and <a href="http://ant.apache.org" target="_blank">Ant</a>.</td>
    </tr>
    </table>
    <hr size="1"/>
</xsl:template>

<xsl:template name="display-time">
    <xsl:param name="value"/>
    <xsl:value-of select="format-number($value,'0.000')"/>
</xsl:template>

<xsl:template name="display-percent">
    <xsl:param name="value"/>
    <xsl:value-of select="format-number($value,'0.00%')"/>
</xsl:template>
</xsl:stylesheet>

