package com.meteor.shared;

/**
 * 컨텍스트 경계를 넘어 공유되는 enum 은 core-shared 에 둔다. api, application, domain, storage 어디서든 참조할 수
 * 있다.
 */
public enum SampleStatus {

    ACTIVE, INACTIVE

}
