<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=social.displayInfo; section>
    <#if section = "title">
        ${msg("loginTitle",(realm.displayName!''))}
    <#elseif section = "header">
        <link href="https://fonts.googleapis.com/css?family=Muli" rel="stylesheet"/>
        <link href="${url.resourcesPath}/img/favicon.png" rel="icon"/>
        <script>
            window.onload = function (e) {
                var mobileNumber = sessionStorage.getItem("mobile_number");
                document.getElementById("mobile_number").value = mobileNumber;
                document.getElementById("mobile-label").innerText = "Enter the code sent to " + mobileNumber;
                if(window.location.protocol === "https:") {
                    let formField = document.getElementById("kc-form-login");
                    if (formField) {
                        formField.action = formField.action.replace("http:","https:");
                    }
                }
            }
        </script>
    <#elseif section = "form">
        <h3>Verify with OTP</h3>
        <div class="ndear-login-card-wrapper">
            <b id="mobile-label">Enter the code sent to </b>
            <div class="box-container">
                <#if realm.password>
                    <div>
                        <form id="kc-form-login" class="form" onsubmit="login.disabled = true; return true;"
                              action="${url.loginAction}" method="post">
                            <div class="input-wrapper">
                                <div class="input-field mobile d-none">
                                    <label for="mobile_number" class="mobile-prefix">+91</label>
                                    <input id="mobile_number" class="login-field" placeholder="XXXXXXXXXX"
                                           type="text"
                                           name="mobile_number"
                                           tabindex="1"/>
                                </div>

                                <div class="input-field mobile">
                                    <input id="otp" class="login-field" placeholder="XXXX"
                                           type="password"
                                           name="otp" tabindex="2">
                                </div>
                            </div>
                            <div class="mt-2">Didn’t receive code? <a class="register-link" onclick="window.location.reload()">Send again</a></div>
                            <input type="hidden" id="type-hidden-input" name="form_type" value="verify_otp"/>
                            <button class="submit" type="submit" tabindex="3">
                                <span>Verify</span>
                            </button>
                        </form>
                    </div>
                </#if>


            </div>
        </div>
    </#if>
</@layout.registrationLayout>
