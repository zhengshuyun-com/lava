/*
 * Copyright 2026 整数科技 (zhengshuyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zhengshuyun.lava.http;

import com.zhengshuyun.lava.core.lang.ValidationUtils;
import okhttp3.HttpUrl;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * 完整的 HTTP/HTTPS 地址构建器。
 *
 * <p>该类型使用 JDK {@link URI} 执行输入语法校验，使用 OkHttp 的 HTTP URL 规则完成规范化和编码。
 * 它不是对原始字符串的逐字符拼接：协议与主机会规范为小写，默认端口可能省略，路径点段会归一化，
 * 不合法或未编码的字符也可能被转义。需要参与签名或要求字节完全一致的 URL，必须先确认规范化后的结果
 * 是否符合目标协议。
 *
 * <p>{@link #from(String)} 接收完整地址并保留已有组成部分。基础地址是否允许用户信息、查询参数或片段
 * 属于具体业务规则，应由调用方在进入构建器前校验；本类不会静默删除输入中的任何组成部分。片段可以
 * 保存在最终 URI 中，但 HTTP 请求不会把 {@code #fragment} 发送给服务端。
 *
 * <p>未带 {@code encoded} 的方法接收普通文本并负责百分号编码；带 {@code encoded} 的方法假设调用方已经
 * 正确编码。两类方法不能混用，否则容易出现重复编码，例如把 {@code %2F} 变成 {@code %252F}。
 *
 * <p>{@link #appendPath(String)} 使用路径追加语义，开头的 {@code /} 不会像
 * {@link URI#resolve(String)} 那样直接回到站点根路径。但 {@code .}、{@code ..}、对应的编码点段以及反斜杠
 * 仍按 OkHttp 路径规则处理，可能归一化现有路径；不能把未经校验的用户输入当作可信路径前缀追加。
 *
 * <p>构建器可在 {@link #build()} 后继续修改和再次构建，但自身是可变对象，不保证线程安全。
 */
public final class HttpUrlBuilder {

    /** 底层 HTTP 地址构建器，不向公共 API 暴露 OkHttp 类型。 */
    private final HttpUrl.Builder delegate;

    private HttpUrlBuilder(HttpUrl.Builder delegate) {
        this.delegate = delegate;
    }

    /**
     * 从协议和主机创建一个新的 HTTP/HTTPS 地址构建器。
     *
     * <p>协议只接受 {@code http} 或 {@code https}，大小写会被规范化。主机参数只填写域名或 IP，
     * 不得混入协议、端口、路径、查询参数；国际化域名会转换为 ASCII 形式。
     *
     * @param scheme HTTP 或 HTTPS 协议
     * @param host 主机名或 IP 地址
     * @return 地址构建器
     * @throws IllegalArgumentException 协议或主机为空，或格式无效
     */
    public static HttpUrlBuilder create(String scheme, String host) {
        HttpUrl.Builder builder = new HttpUrl.Builder()
                .scheme(ValidationUtils.requireNotBlank(scheme, "scheme must not be blank"))
                .host(ValidationUtils.requireNotBlank(host, "host must not be blank"));
        return new HttpUrlBuilder(builder);
    }

    /**
     * 从完整 HTTP/HTTPS 地址创建构建器，并保留已有的全部组成部分。
     *
     * <p>输入必须先满足 JDK URI 语法，因此空格、非法百分号编码等内容不会被自动修复。解析成功后仍会按照
     * HTTP URL 规则规范化，构建结果与输入字符串可能不完全相同；路径、查询参数和片段的语义会保留。
     *
     * @param url 完整的绝对 HTTP/HTTPS 地址
     * @return 地址构建器
     * @throws IllegalArgumentException 地址为空、格式无效或不是 HTTP/HTTPS 地址
     */
    public static HttpUrlBuilder from(String url) {
        return new HttpUrlBuilder(requireHttpUrl(url, "url").newBuilder());
    }

    /**
     * 从完整 HTTP/HTTPS 地址创建构建器，并保留已有的全部组成部分。
     *
     * <p>传入的 URI 仍会经过 HTTP URL 规范化，不保证 {@link URI#toString()} 与最终结果逐字符一致。
     *
     * @param url 完整的绝对 HTTP/HTTPS 地址
     * @return 地址构建器
     * @throws IllegalArgumentException 地址为空、格式无效或不是 HTTP/HTTPS 地址
     */
    public static HttpUrlBuilder from(URI url) {
        ValidationUtils.requireNonNull(url, "url must not be null");
        return from(url.toString());
    }

    /**
     * 替换协议。
     *
     * <p>切换协议不会主动删除已经显式设置的端口。例如从 HTTP 的 {@code :8080} 切换到 HTTPS 后仍会
     * 保留 {@code :8080}；需要协议默认端口时应继续调用 {@link #defaultPort()}。
     *
     * @param scheme HTTP 或 HTTPS 协议
     * @return 当前构建器
     * @throws IllegalArgumentException 协议为空或不是 HTTP/HTTPS
     */
    public HttpUrlBuilder scheme(String scheme) {
        delegate.scheme(ValidationUtils.requireNotBlank(scheme, "scheme must not be blank"));
        return this;
    }

    /**
     * 替换用户名；传入空字符串可移除已有用户名。
     *
     * <p>用户名属于 URL user-info，会出现在 URI 文本中，不适合存放敏感凭据。参数按未编码文本处理；
     * 已经带百分号编码的内容应使用 {@link #encodedUsername(String)}。
     *
     * @param username 未编码用户名
     * @return 当前构建器
     * @throws IllegalArgumentException 用户名为 {@code null}
     */
    public HttpUrlBuilder username(String username) {
        delegate.username(ValidationUtils.requireNonNull(username, "username must not be null"));
        return this;
    }

    /**
     * 替换已经编码的用户名；传入空字符串可移除已有用户名。
     *
     * <p>调用方负责保证编码符合 URL user-info 规则。该值仍可能出现在日志或异常中，不建议用于承载凭据。
     *
     * @param username 已编码用户名
     * @return 当前构建器
     * @throws IllegalArgumentException 用户名为 {@code null}
     */
    public HttpUrlBuilder encodedUsername(String username) {
        delegate.encodedUsername(ValidationUtils.requireNonNull(username, "username must not be null"));
        return this;
    }

    /**
     * 替换密码；传入空字符串可移除已有密码。
     *
     * <p>密码属于 URL user-info，会直接体现在 URI 文本中。HTTP API 凭据通常应通过认证请求头传递，
     * 不应使用该方法。参数按未编码文本处理。
     *
     * @param password 未编码密码
     * @return 当前构建器
     * @throws IllegalArgumentException 密码为 {@code null}
     */
    public HttpUrlBuilder password(String password) {
        delegate.password(ValidationUtils.requireNonNull(password, "password must not be null"));
        return this;
    }

    /**
     * 替换已经编码的密码；传入空字符串可移除已有密码。
     *
     * <p>调用方负责保证编码正确。即使经过编码，密码仍不是脱敏内容，不能安全地写入日志。
     *
     * @param password 已编码密码
     * @return 当前构建器
     * @throws IllegalArgumentException 密码为 {@code null}
     */
    public HttpUrlBuilder encodedPassword(String password) {
        delegate.encodedPassword(ValidationUtils.requireNonNull(password, "password must not be null"));
        return this;
    }

    /**
     * 替换主机名或 IP 地址。
     *
     * <p>只传主机部分，不包含协议、端口和路径。域名会转为小写，国际化域名会规范为 ASCII；
     * IPv6 地址由底层 HTTP URL 实现负责规范化。
     *
     * @param host 主机名或 IP 地址
     * @return 当前构建器
     * @throws IllegalArgumentException 主机为空或格式无效
     */
    public HttpUrlBuilder host(String host) {
        delegate.host(ValidationUtils.requireNotBlank(host, "host must not be blank"));
        return this;
    }

    /**
     * 替换端口。
     *
     * <p>端口必须在 1 至 65535 之间。显式设置为当前协议的默认端口时，最终 URI 文本可能省略该端口。
     *
     * @param port 端口号，范围为 1 至 65535
     * @return 当前构建器
     * @throws IllegalArgumentException 端口不在有效范围内
     */
    public HttpUrlBuilder port(int port) {
        delegate.port(port);
        return this;
    }

    /**
     * 将端口恢复为当前协议的默认端口。
     *
     * <p>HTTP 使用 80，HTTPS 使用 443。默认端口通常不会出现在最终 URI 文本中。
     *
     * @return 当前构建器
     */
    public HttpUrlBuilder defaultPort() {
        HttpUrl current = delegate.build();
        delegate.port(HttpUrl.defaultPort(current.scheme()));
        return this;
    }

    /**
     * 替换完整路径。
     *
     * <p>参数按未编码路径处理，开头的一个 {@code /} 可省略，空字符串表示根路径。额外的连续斜杠
     * 表示空路径段并会保留，例如 {@code //api} 构建后仍为 {@code //api}。斜杠用于分隔路径段，需要
     * 把斜杠作为单个段的数据时应使用 {@link #appendPathSegment(String)}。
     *
     * <p>{@code .}、{@code ..} 和反斜杠按照 OkHttp 路径规则归一化。因此该方法不是原始路径字符串的
     * 无损设置入口；需要传入现成百分号编码时使用 {@link #encodedPath(String)}。
     *
     * @param path 未编码路径
     * @return 当前构建器
     * @throws IllegalArgumentException 路径为 {@code null}
     */
    public HttpUrlBuilder path(String path) {
        ValidationUtils.requireNonNull(path, "path must not be null");
        delegate.encodedPath("/");
        int start = path.startsWith("/") ? 1 : 0;
        if (start < path.length()) {
            delegate.addPathSegments(path.substring(start));
        }
        return this;
    }

    /**
     * 替换完整的已编码路径。
     *
     * <p>开头的 {@code /} 可省略，空字符串表示根路径。已有百分号编码不会重复编码，连续斜杠会作为
     * 空路径段保留；但点段、编码点段和反斜杠仍可能被底层 HTTP URL 规则归一化。
     *
     * <p>该方法只适合已经按 URL path 规则编码的完整路径。传入普通文本可能导致保留字符获得错误语义。
     *
     * @param path 已编码路径
     * @return 当前构建器
     * @throws IllegalArgumentException 路径为 {@code null}
     */
    public HttpUrlBuilder encodedPath(String path) {
        ValidationUtils.requireNonNull(path, "path must not be null");
        delegate.encodedPath(path.startsWith("/") ? path : "/" + path);
        return this;
    }

    /**
     * 将路径追加到现有路径后面。
     *
     * <p>连接处连续的 {@code /} 会折叠为一个，路径内容会按 URL 路径规则编码，末尾的 {@code /}
     * 会被保留；待追加内容内部的连续斜杠仍会作为空路径段保留。
     *
     * <p><strong>注意：</strong>{@code .}、{@code ..} 和反斜杠具有路径语义，可能归一化甚至回退现有路径。
     * 本方法不能作为防止路径穿越的安全边界，调用方必须先校验不可信输入。需要追加一个不允许斜杠充当
     * 分隔符的值时，应使用 {@link #appendPathSegment(String)}。
     *
     * <p>空字符串不执行任何修改；只包含空白的内容是合法路径数据，会按规则进行百分号编码。
     *
     * @param path 待追加的未编码路径
     * @return 当前构建器
     * @throws IllegalArgumentException 路径为 {@code null}
     */
    public HttpUrlBuilder appendPath(String path) {
        ValidationUtils.requireNonNull(path, "path must not be null");
        if (path.isEmpty()) {
            return this;
        }
        return appendPathValue(path, false);
    }

    /**
     * 将已经编码的路径追加到现有路径后面。
     *
     * <p>连接处连续的 {@code /} 会折叠为一个，已有百分号编码不会被重复编码。该方法不会验证调用方是否
     * 正确编码，普通文本传入这里可能获得与 {@link #appendPath(String)} 不同的结果。
     *
     * <p><strong>注意：</strong>{@code .}、{@code ..}、{@code %2e} 等编码点段和反斜杠仍会按 OkHttp
     * 路径规则归一化，可能回退现有路径；不能直接用于未经校验的外部输入。
     *
     * <p>空字符串不执行任何修改。该方法只适合已经按 URL path 规则编码的内容。
     *
     * @param path 待追加的已编码路径
     * @return 当前构建器
     * @throws IllegalArgumentException 路径为 {@code null}
     */
    public HttpUrlBuilder appendEncodedPath(String path) {
        ValidationUtils.requireNonNull(path, "path must not be null");
        if (path.isEmpty()) {
            return this;
        }
        return appendPathValue(path, true);
    }

    /**
     * 追加一个路径段；段内的 {@code /} 会被编码而不会作为路径分隔符。
     *
     * <p>该方法直接采用 OkHttp 的路径段语义，不会删除现有空路径段。段内普通斜杠和反斜杠会作为数据
     * 编码；但值恰好为 {@code .} 或 {@code ..} 时，底层仍会执行点段归一化，而不是追加字面值。
     *
     * @param segment 未编码路径段
     * @return 当前构建器
     * @throws IllegalArgumentException 路径段为 {@code null}
     */
    public HttpUrlBuilder appendPathSegment(String segment) {
        delegate.addPathSegment(ValidationUtils.requireNonNull(segment, "segment must not be null"));
        return this;
    }

    /**
     * 追加一个已经编码的路径段；段内的 {@code /} 会被编码而不会作为路径分隔符。
     *
     * <p>该方法不会删除现有空路径段。调用方负责提供正确的编码；{@code %2F} 可作为段内斜杠，但
     * {@code %2e}、{@code %2e%2e} 等编码点段仍具有归一化语义，可能修改已有路径。
     *
     * @param segment 已编码路径段
     * @return 当前构建器
     * @throws IllegalArgumentException 路径段为 {@code null}
     */
    public HttpUrlBuilder appendEncodedPathSegment(String segment) {
        delegate.addEncodedPathSegment(ValidationUtils.requireNonNull(segment, "segment must not be null"));
        return this;
    }

    /**
     * 替换完整查询串，并按未编码内容处理。
     *
     * <p>参数不包含开头的 {@code ?}。其中的 {@code &} 和 {@code =} 仍作为查询结构分隔符，普通文本会
     * 进行百分号编码；已经编码的百分号会再次编码，例如 {@code %2F} 会变成 {@code %252F}。需要保留
     * 现成查询串时使用 {@link #encodedQuery(String)}，需要安全编码单个名称和值时优先使用查询参数方法。
     *
     * @param query 不含开头 {@code ?} 的查询串；传入 {@code null} 可移除查询串
     * @return 当前构建器
     */
    public HttpUrlBuilder query(@Nullable String query) {
        delegate.query(query);
        return this;
    }

    /**
     * 替换完整的已编码查询串。
     *
     * <p>参数不包含开头的 {@code ?}。参数顺序、重复参数、空参数和已有百分号编码均按已编码查询串处理，
     * 适合转发 {@code HttpServletRequest#getQueryString()} 一类未解码值。调用方必须保证内容已经正确编码；
     * {@code null} 表示完全移除 query，空字符串则保留一个空 query，即 URL 末尾仍可能出现 {@code ?}。
     *
     * @param query 已编码查询串；传入 {@code null} 可移除查询串
     * @return 当前构建器
     */
    public HttpUrlBuilder encodedQuery(@Nullable String query) {
        delegate.encodedQuery(query);
        return this;
    }

    /**
     * 追加查询参数，不移除已有同名参数。
     *
     * <p>名称和值按普通文本编码，因此值中的 {@code +} 会编码为 {@code %2B}，不会被误认为表单空格。
     * {@code null} 值生成 {@code name}，空字符串生成 {@code name=}，二者语义不同。
     *
     * <p>参数名可以是空字符串；这会生成 RFC 3986 允许的空名称参数，例如 {@code ?=value}。
     *
     * @param name 未编码参数名
     * @param value 未编码参数值；传入 {@code null} 表示无等号和值的参数
     * @return 当前构建器
     * @throws IllegalArgumentException 参数名为 {@code null}
     */
    public HttpUrlBuilder addQueryParam(String name, @Nullable String value) {
        delegate.addQueryParameter(requireQueryName(name), value);
        return this;
    }

    /**
     * 替换全部同名查询参数。
     *
     * <p>名称和值按普通文本编码。已有同名参数会全部移除，再在查询串末尾添加新值，因此参数位置可能变化。
     * {@code null} 值表示无等号和值的参数，不表示删除；删除应使用 {@link #removeQueryParam(String)}。
     *
     * <p>参数名可以是空字符串；这会生成 RFC 3986 允许的空名称参数。
     *
     * @param name 未编码参数名
     * @param value 未编码参数值；传入 {@code null} 表示无等号和值的参数
     * @return 当前构建器
     * @throws IllegalArgumentException 参数名为 {@code null}
     */
    public HttpUrlBuilder queryParam(String name, @Nullable String value) {
        delegate.setQueryParameter(requireQueryName(name), value);
        return this;
    }

    /**
     * 追加已经编码的查询参数，不移除已有同名参数。
     *
     * <p>名称和值都必须是已经编码的内容。该方法不会把 {@code +} 改成 {@code %2B}；如果目标服务按
     * {@code application/x-www-form-urlencoded} 规则解析查询串，{@code +} 可能被解释为空格。
     *
     * <p>已编码参数名可以是空字符串。
     *
     * @param name 已编码参数名
     * @param value 已编码参数值；传入 {@code null} 表示无等号和值的参数
     * @return 当前构建器
     * @throws IllegalArgumentException 参数名为 {@code null}
     */
    public HttpUrlBuilder addEncodedQueryParam(String name, @Nullable String value) {
        delegate.addEncodedQueryParameter(requireQueryName(name), value);
        return this;
    }

    /**
     * 替换全部同名的已编码查询参数。
     *
     * <p>匹配和新增都按编码后的参数名执行。已有同名参数会全部移除，新值追加到查询串末尾；
     * {@code null} 值生成无等号和值的参数，不表示删除。
     *
     * <p>已编码参数名可以是空字符串。
     *
     * @param name 已编码参数名
     * @param value 已编码参数值；传入 {@code null} 表示无等号和值的参数
     * @return 当前构建器
     * @throws IllegalArgumentException 参数名为 {@code null}
     */
    public HttpUrlBuilder encodedQueryParam(String name, @Nullable String value) {
        delegate.setEncodedQueryParameter(requireQueryName(name), value);
        return this;
    }

    /**
     * 移除全部同名查询参数。
     *
     * <p>参数名按普通文本编码后匹配，适合与 {@link #addQueryParam(String, String)} 和
     * {@link #queryParam(String, String)} 配套使用。找不到参数时保持原 URL 不变。
     *
     * @param name 未编码参数名
     * @return 当前构建器
     * @throws IllegalArgumentException 参数名为 {@code null}
     */
    public HttpUrlBuilder removeQueryParam(String name) {
        delegate.removeAllQueryParameters(requireQueryName(name));
        return this;
    }

    /**
     * 移除全部同名的已编码查询参数。
     *
     * <p>参数名按已编码形式匹配，适合与编码查询参数方法配套使用。不要把普通文本误传给该方法，
     * 否则可能无法匹配预期参数。
     *
     * @param name 已编码参数名
     * @return 当前构建器
     * @throws IllegalArgumentException 参数名为 {@code null}
     */
    public HttpUrlBuilder removeEncodedQueryParam(String name) {
        delegate.removeAllEncodedQueryParameters(requireQueryName(name));
        return this;
    }

    /**
     * 替换片段，并按未编码内容处理。
     *
     * <p>参数不包含开头的 {@code #}。片段会保存在构建出的 URI 中，但 HTTP 请求不会发送它；
     * 请求上游接口时通常应保持为 {@code null}。已经编码的值应使用 {@link #encodedFragment(String)}。
     *
     * @param fragment 片段；传入 {@code null} 可移除片段
     * @return 当前构建器
     */
    public HttpUrlBuilder fragment(@Nullable String fragment) {
        delegate.fragment(fragment);
        return this;
    }

    /**
     * 替换已经编码的片段。
     *
     * <p>参数不包含开头的 {@code #}，调用方负责保证编码正确。片段只属于客户端 URI 表示，
     * 不会成为 HTTP 请求目标的一部分。
     *
     * @param fragment 已编码片段；传入 {@code null} 可移除片段
     * @return 当前构建器
     */
    public HttpUrlBuilder encodedFragment(@Nullable String fragment) {
        delegate.encodedFragment(fragment);
        return this;
    }

    /**
     * 构建不可变的 HTTP/HTTPS URI。
     *
     * <p>返回值是规范化后的快照，可能与最初输入的文本不同。调用该方法不会冻结当前构建器，后续仍可继续
     * 修改并再次构建；多线程之间不能共享同一个构建器实例。
     *
     * @return 构建完成的 URI
     */
    public URI build() {
        return delegate.build().uri();
    }

    private HttpUrlBuilder appendPathValue(String path, boolean encoded) {
        normalizeAppendBoundary();
        int start = 0;
        while (start < path.length() && path.charAt(start) == '/') {
            start++;
        }
        if (start == path.length()) {
            appendTrailingSlash();
            return this;
        }
        String value = path.substring(start);
        if (encoded) {
            delegate.addEncodedPathSegments(value);
        } else {
            delegate.addPathSegments(value);
        }
        return this;
    }

    /**
     * 折叠现有路径末尾的重复斜杠，使下一次追加只有一个连接符。
     */
    private void normalizeAppendBoundary() {
        String path = delegate.build().encodedPath();
        int end = path.length();
        while (end > 1 && path.charAt(end - 1) == '/') {
            end--;
        }
        delegate.encodedPath(path.substring(0, end));
    }

    /**
     * 在当前路径末尾补一个斜杠，同时避免把根路径变成双斜杠。
     */
    private void appendTrailingSlash() {
        String path = delegate.build().encodedPath();
        if (!path.endsWith("/")) {
            delegate.encodedPath(path + "/");
        }
    }

    private static HttpUrl requireHttpUrl(String url, String parameterName) {
        ValidationUtils.requireNotBlank(url, parameterName + " must not be blank");
        URI value;
        try {
            value = new URI(url);
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException(parameterName + " must be a valid URI", exception);
        }
        if (!value.isAbsolute()
                || !("http".equalsIgnoreCase(value.getScheme())
                || "https".equalsIgnoreCase(value.getScheme()))) {
            throw new IllegalArgumentException(
                    parameterName + " must be an absolute HTTP or HTTPS URL");
        }
        HttpUrl parsed = HttpUrl.parse(value.toString());
        if (parsed == null) {
            throw new IllegalArgumentException(parameterName + " must be a valid HTTP or HTTPS URL");
        }
        return parsed;
    }

    private static String requireQueryName(String name) {
        return ValidationUtils.requireNonNull(name, "query parameter name must not be null");
    }
}
