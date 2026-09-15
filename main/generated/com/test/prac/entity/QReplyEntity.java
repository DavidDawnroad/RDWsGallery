package com.test.prac.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QReplyEntity is a Querydsl query type for ReplyEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QReplyEntity extends EntityPathBase<ReplyEntity> {

    private static final long serialVersionUID = -648463811L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QReplyEntity replyEntity = new QReplyEntity("replyEntity");

    public final QBaseEntity _super = new QBaseEntity(this);

    public final QBoardEntity boardEntity;

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> moddate = _super.moddate;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> regdate = _super.regdate;

    public final NumberPath<Long> replySeq = createNumber("replySeq", Long.class);

    public final QUserAccountEntity userEntity;

    public QReplyEntity(String variable) {
        this(ReplyEntity.class, forVariable(variable), INITS);
    }

    public QReplyEntity(Path<? extends ReplyEntity> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QReplyEntity(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QReplyEntity(PathMetadata metadata, PathInits inits) {
        this(ReplyEntity.class, metadata, inits);
    }

    public QReplyEntity(Class<? extends ReplyEntity> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.boardEntity = inits.isInitialized("boardEntity") ? new QBoardEntity(forProperty("boardEntity"), inits.get("boardEntity")) : null;
        this.userEntity = inits.isInitialized("userEntity") ? new QUserAccountEntity(forProperty("userEntity")) : null;
    }

}

